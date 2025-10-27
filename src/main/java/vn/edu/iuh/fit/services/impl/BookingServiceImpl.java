package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.BookingDto;
import vn.edu.iuh.fit.dtos.requests.BookingCreateRequest;
import vn.edu.iuh.fit.dtos.requests.PaymentWebhookPayload;
import vn.edu.iuh.fit.entities.*;
import vn.edu.iuh.fit.entities.enums.BookingStatus;
import vn.edu.iuh.fit.entities.enums.ContractStatus;
import vn.edu.iuh.fit.entities.enums.InvoiceStatus;
import vn.edu.iuh.fit.mappers.BookingMapper;
import vn.edu.iuh.fit.payments.PaymentGateway;
import vn.edu.iuh.fit.payments.PaymentLink;
import vn.edu.iuh.fit.repositories.*;
import vn.edu.iuh.fit.services.BookingService;
import vn.edu.iuh.fit.services.RealtimeNotificationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepo;
    private final PropertyRepository propertyRepo;
    private final UserRepository userRepo;
    private final InvoiceRepository invoiceRepo;
    private final ContractRepository contractRepo;
    private final BookingMapper bookingMapper;
    private final PaymentGateway paymentGateway;
    private final SimpMessagingTemplate messaging;
    private final RealtimeNotificationService realtimeNotificationService;
    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);

    @Transactional
    @Override
    public BookingDto createDaily(String tenantId, BookingCreateRequest req) {
        if(req.getPropertyId() == null || req.getCheckInAt() == null || req.getCheckOutAt() == null) {
            throw new IllegalArgumentException("propertyId, checkInAt, checkOutAt are required");
        }

        LocalDate checkInDate = req.getCheckInAt();
        LocalDate checkOutDate = req.getCheckOutAt();
        if (!checkOutDate.isAfter(checkInDate)){
            throw new IllegalArgumentException("checkOutAt must be after checkInAt");
        }

        Property property = propertyRepo.findById(req.getPropertyId()).orElseThrow();
        User tenant = userRepo.findById(tenantId).orElseThrow();

        if (bookingRepo.existsOverlap(property.getPropertyId(), checkInDate, checkOutDate)) {
            throw new IllegalStateException("Property not available in selected dates");
        }
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (nights <= 0) {
            throw new IllegalArgumentException("At least 1 night is required");
        }

        BigDecimal pricePerNight = property.getPrice();
        BigDecimal total = pricePerNight.multiply(BigDecimal.valueOf(nights));
        LocalDateTime now = LocalDateTime.now();

        Booking booking = new Booking();
        booking.setBookingId(UUID.randomUUID().toString());
        booking.setProperty(property);
        booking.setTenant(tenant);
        booking.setStartDate(checkInDate);
        booking.setEndDate(checkOutDate);
        booking.setNote(req.getNote());
        booking.setTotalPrice(total);
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);

        Booking savedBooking = bookingRepo.save(booking);

        Invoice invoice = invoiceRepo.findByBooking_BookingId(savedBooking.getBookingId())
                .orElseGet(() -> {
                    Invoice inv = new Invoice();
                    inv.setInvoiceId(UUID.randomUUID().toString());
                    inv.setBooking(savedBooking);
                    inv.setInvoiceNo(genInvoiceNo());
                    inv.setCreatedAt(LocalDateTime.now());
                    return inv;
                });
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(now);
        invoice.setDueAt(now.plusHours(24)); // hạn thanh toán: 24h
        invoice.setTotal(total);
        invoice.setSubtotal(total);
        if (invoice.getTax() == null){
            invoice.setTax(BigDecimal.ZERO);
        }
        if (invoice.getDiscount() == null){
            invoice.setDiscount(BigDecimal.ZERO);
        }
        invoice.setDueAmount(total);
        invoice.setUpdatedAt(now);
        invoice.setTenantName(tenant.getFullName());
        invoice.setTenantEmail(tenant.getEmail());
        invoice.setTenantPhone(tenant.getPhoneNumber());
        invoice.setLandlordName(property.getLandlord() != null ? property.getLandlord().getFullName() : null);
        invoice.setLandlordEmail(property.getLandlord() != null ? property.getLandlord().getEmail() : null);
        invoice.setLandlordPhone(property.getLandlord() != null ? property.getLandlord().getPhoneNumber() : null);
        invoice.setPropertyTitle(property.getTitle());
        invoice.setPropertyAddressText(property.getAddress() != null ? property.getAddress().getAddressFull() : null);
        invoice.setCancellationFee(BigDecimal.ZERO);
        invoice.setRefundableAmount(BigDecimal.ZERO);
        invoice.setRefundConfirmed(Boolean.FALSE);
        invoice.setRefundRequestedAt(null);
        invoice.setRefundConfirmedAt(null);

        BigDecimal roundedTotal = total.setScale(0, RoundingMode.HALF_UP);
        long amount = roundedTotal.longValueExact();
        PaymentLink link = paymentGateway.createPayment(
                invoice.getInvoiceId(),
                amount,
                invoice.getInvoiceNo(),
                "https://your-frontend.com/payment/success",
                "https://blackishly-unequalled-selina.ngrok-free.dev/api/v1/payments/webhook"

        );
        invoice.setPaymentUrl(link.getCheckoutUrl());
        invoice.setQrPayload(link.getQrPayload());
        invoice.setPaymentRef(String.valueOf(link.getOrderCode()));
        invoice.setPaymentMethod("PAYOS");
        invoiceRepo.save(invoice);

        savedBooking.setPaymentUrl(link.getCheckoutUrl());
        savedBooking.setUpdatedAt(now);
        bookingRepo.save(savedBooking);

        //Contract auto
        Contract contract = contractRepo.findByBooking_BookingId(savedBooking.getBookingId())
                .orElseGet(() -> {
                    Contract c = new Contract();
                    c.setCreatedAt(now);
                    return c;
                });
        contract.setBooking(savedBooking);
        contract.setTenantName(tenant.getFullName());
        contract.setTenantPhone(tenant.getPhoneNumber());
        contract.setTitle("Hợp đồng thuê " + property.getTitle() + " - " + tenant.getFullName());
        contract.setRoomNumber(property.getRoomNumber());
        contract.setBuildingName(property.getBuildingName());
        contract.setStartDate(checkInDate);
        contract.setEndDate(checkOutDate);
        contract.setRentPrice(property.getPrice());
        contract.setDeposit(property.getDeposit());
        contract.setBillingStartDate(LocalDate.now());
        contract.setPaymentDueDay(null);
        contract.setNotes(property.getDescription());
        contract.setContractStatus(ContractStatus.PENDING_REVIEW);
        contract.setUpdatedAt(now);

        if (contract.getServices() != null) {
            contract.getServices().clear();
        } else {
            contract.setServices(new ArrayList<>());
        }
        if (property.getServices() != null) {
            for (PropertyServiceItem item : property.getServices()) {
                ContractService contractService = ContractService.builder()
                        .serviceName(item.getServiceName())
                        .fee(item.getFee())
                        .chargeBasis(item.getChargeBasis())
                        .isIncluded(item.getIsIncluded())
                        .note(item.getNote())
                        .contract(contract)
                        .build();
                contract.getServices().add(contractService);
            }
        }

        contractRepo.save(contract);
        savedBooking.setContract(contract);

        BookingDto result = bookingMapper.toDto(savedBooking);
        realtimeNotificationService.notifyLandlordBookingCreated(result);

        return result;
    }

    @Override
    public BookingDto approve(String bookingId, String landlordId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        if (booking.getProperty() == null || booking.getProperty().getLandlord() == null
                || !booking.getProperty().getLandlord().getUserId().equals(landlordId)) {
            throw new SecurityException("Only landlord can approve this booking");
        }

        booking.setBookingStatus(BookingStatus.AWAITING_LANDLORD_APPROVAL);
        booking.setUpdatedAt(LocalDateTime.now());
        Booking saved = bookingRepo.save(booking);

        contractRepo.findByBooking_BookingId(bookingId).ifPresent(contract -> {
            contract.setContractStatus(ContractStatus.PENDING_REVIEW);
            contract.setUpdatedAt(LocalDateTime.now());
            contractRepo.save(contract);
        });

        // Notify tenant about booking approval
        BookingDto bookingDto = bookingMapper.toDto(saved);
        realtimeNotificationService.notifyTenantBookingApproved(bookingDto);

        return bookingDto;
    }

    @Override
    @Transactional
    public BookingDto cancel(String bookingId, String tenantId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();

        boolean isTenant = booking.getTenant() != null && booking.getTenant().getUserId().equals(tenantId);
        boolean isLandlord = booking.getProperty() != null && booking.getProperty().getLandlord() != null
                && booking.getProperty().getLandlord().getUserId().equals(tenantId);  // Sử dụng tenantId cho landlord vì param là userId

        if (!isTenant && !isLandlord) {
            throw new SecurityException("Only tenant or landlord can cancel this booking");
        }

        if (EnumSet.of(BookingStatus.CANCELLED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED)
                .contains(booking.getBookingStatus())) {
            throw new IllegalStateException("Không thể hủy đặt phòng ở giai đoạn này");
        }
        LocalDateTime now = LocalDateTime.now();
        Invoice invoice = invoiceRepo.findByBooking_BookingId(bookingId).orElse(null);

        if (booking.getBookingStatus() == BookingStatus.APPROVED) {
            applyRefundPolicy(booking, invoice, now);
        } else {
            if (invoice != null) {
                invoice.setStatus(InvoiceStatus.VOID);
                invoice.setCancellationFee(BigDecimal.ZERO);
                invoice.setRefundableAmount(BigDecimal.ZERO);
                invoice.setRefundConfirmed(Boolean.TRUE);
                invoice.setRefundRequestedAt(null);
                invoice.setRefundConfirmedAt(now);
                invoice.setUpdatedAt(now);
                invoiceRepo.save(invoice);
            }
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(now);
        bookingRepo.save(booking);

        contractRepo.findByBooking_BookingId(bookingId).ifPresent(contract -> {
            contract.setContractStatus(ContractStatus.CANCELLED);
            contract.setUpdatedAt(now);
            contractRepo.save(contract);
        });

        return bookingMapper.toDto(booking);
    }

    @Override
    public BookingDto checkIn(String bookingId, String userId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        boolean isTenant = booking.getTenant() != null && booking.getTenant().getUserId().equals(userId);
        boolean isLandlord = booking.getProperty() != null &&
                booking.getProperty().getLandlord() != null &&
                booking.getProperty().getLandlord().getUserId().equals(userId);

        if (!isTenant && !isLandlord) {
            throw new SecurityException("Only tenant or landlord can check-in");
        }
        if (booking.getBookingStatus() != BookingStatus.APPROVED) {
            throw new IllegalStateException("Booking chưa được thanh toán hoặc đã ở trạng thái khác");
        }

        LocalDate startDate = booking.getStartDate();
        if (startDate == null) {
            throw new IllegalStateException("Booking chưa có ngày nhận phòng");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime standardCheckIn = startDate.atTime(14, 0);
        LocalDateTime earlyCheckInStart = standardCheckIn.minusMinutes(10);

        if (now.isBefore(earlyCheckInStart)) {
            throw new IllegalStateException("Chỉ được nhận phòng từ 14:00. Có thể check-in sớm nhất từ 13:50-14:00 nếu phòng trống.");
        }

        if (now.isBefore(standardCheckIn)) {
            Property property = booking.getProperty();
            if (property == null || property.getPropertyId() == null) {
                throw new IllegalStateException("Không xác định được căn phòng để kiểm tra tình trạng");
            }

            boolean previousGuestNotCheckedOut = bookingRepo.existsActiveBookingEndingOn(
                    property.getPropertyId(),
                    startDate,
                    booking.getBookingId()
            );

            if (previousGuestNotCheckedOut) {
                throw new IllegalStateException("Khách thuê trước chưa trả phòng, vui lòng check-in sau 14:00.");
            }
        }

        booking.setBookingStatus(BookingStatus.CHECKED_IN);
        booking.setUpdatedAt(LocalDateTime.now());

        Booking savedBooking = bookingRepo.save(booking);
        BookingDto result = bookingMapper.toDto(savedBooking);
        realtimeNotificationService.notifyBookingCheckedIn(result);
        return result;
    }

    @Transactional
    @Override
    public BookingDto checkOut(String bookingId, String userId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        boolean isTenant = booking.getTenant() != null && booking.getTenant().getUserId().equals(userId);
        boolean isLandlord = booking.getProperty() != null &&
                booking.getProperty().getLandlord() != null &&
                booking.getProperty().getLandlord().getUserId().equals(userId);

        if (!isTenant && !isLandlord) {
            throw new SecurityException("Only tenant or landlord can check-out");
        }
        if (booking.getBookingStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Booking is not in CHECKED_IN status");
        }
        Invoice invoice = invoiceRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new IllegalStateException("Invoice not found"));
        BigDecimal due = invoice.getDueAmount() == null ? BigDecimal.ZERO : invoice.getDueAmount();

        booking.setBookingStatus(BookingStatus.COMPLETED);
        booking.setUpdatedAt(LocalDateTime.now());

        Booking savedBooking = bookingRepo.save(booking);
        BookingDto result = bookingMapper.toDto(savedBooking);
        realtimeNotificationService.notifyBookingCheckedOut(result);
        return result;
    }

    @Override
    public BookingDto getOne(String bookingId, String userId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        // chỉ tenant hoặc landlord của booking mới được xem
        boolean isTenant = booking.getTenant() != null && booking.getTenant().getUserId().equals(userId);
        boolean isLandlord = booking.getProperty() != null
                && booking.getProperty().getLandlord() != null
                && booking.getProperty().getLandlord().getUserId().equals(userId);

        if (!isTenant && !isLandlord) {
            throw new SecurityException("Not allowed to view this booking");
        }

        return bookingMapper.toDto(booking);
    }

    @Override
    public void handlePaymentWebhook(PaymentWebhookPayload payload) {
        System.out.println("Handling payment webhook: " + payload);
        Invoice invoice = invoiceRepo.findById(payload.getInvoiceId()).orElseThrow();

        if (payload.isSuccess()) {
            invoice.setPaidAt(LocalDateTime.now());
            invoice.setStatus(InvoiceStatus.PAID);
            if (payload.getTransactionId() != null && !payload.getTransactionId().isBlank()) {
                invoice.setPaymentRef(payload.getTransactionId());
            }
            invoice.setCancellationFee(BigDecimal.ZERO);
            invoice.setRefundableAmount(BigDecimal.ZERO);
            invoice.setRefundConfirmed(Boolean.FALSE);
            invoice.setRefundRequestedAt(null);
            invoice.setRefundConfirmedAt(null);
        } else {
            invoice.setStatus(InvoiceStatus.ISSUED);
            invoice.setCancellationFee(BigDecimal.ZERO);
            invoice.setRefundableAmount(BigDecimal.ZERO);
            invoice.setRefundConfirmed(Boolean.FALSE);
            invoice.setRefundRequestedAt(null);
            invoice.setRefundConfirmedAt(null);
        }
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice = invoiceRepo.save(invoice);

        Booking booking = invoice.getBooking();
        Booking savedBooking = booking;
        if (payload.isSuccess()) {
            booking.setBookingStatus(BookingStatus.APPROVED);
            booking.setPaymentUrl(null);
            booking.setUpdatedAt(LocalDateTime.now());
            savedBooking = bookingRepo.save(booking);
            contractRepo.findByBooking_BookingId(booking.getBookingId()).ifPresent(contract -> {
                contract.setContractStatus(ContractStatus.ACTIVE);
                contract.setUpdatedAt(LocalDateTime.now());
                contractRepo.save(contract);
            });
        }

        BookingDto bookingDto = bookingMapper.toDto(savedBooking);
        realtimeNotificationService.notifyPaymentStatusChanged(bookingDto, invoice, payload.isSuccess());

        broadcastPaymentStatus(invoice, savedBooking, payload);
    }

    @Transactional(readOnly = true)
    @Override
    public List<LocalDate> getBookedDates(String propertyId) {
        if (propertyId == null || propertyId.isBlank()) {
            throw new IllegalArgumentException("propertyId is required");
        }

        return bookingRepo.findByProperty_PropertyIdAndBookingStatusNot(propertyId, BookingStatus.CANCELLED)
                .stream()
                .filter(booking -> booking.getStartDate() != null && booking.getEndDate() != null)
                .flatMap(booking -> booking.getStartDate().datesUntil(booking.getEndDate()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private String genInvoiceNo() {
        //INV-LocalDateT.Now()-ABCDEFG (ngẫu nhiên 7 ký tự)
        String date = LocalDate.now().toString().replace("-", "");
        String rand = UUID.randomUUID().toString().substring(0, 7).toUpperCase();
        return "INV-" + date + "-" + rand;
    }

    private void applyRefundPolicy(Booking booking, Invoice invoice, LocalDateTime cancelAt) {
        if (booking == null) {
            return;
        }

        BigDecimal dueAmount = invoice != null ? safe(invoice.getDueAmount()) : BigDecimal.ZERO;
        if (dueAmount.compareTo(BigDecimal.ZERO) <= 0) {
            dueAmount = safe(booking.getTotalPrice());
        }

        BigDecimal refundAmount = dueAmount;
        BigDecimal cancellationFee = BigDecimal.ZERO;

        LocalDate startDate = booking.getStartDate();
        if (startDate != null) {
            LocalDateTime freeCancelDeadline = startDate.minusDays(1).atTime(14, 0);
            LocalDateTime partialRefundDeadline = startDate.atTime(14, 0);

            if (cancelAt.isBefore(freeCancelDeadline)) {
                refundAmount = dueAmount;
                cancellationFee = BigDecimal.ZERO;
            } else if (cancelAt.isBefore(partialRefundDeadline)) {
                BigDecimal firstNightCharge = calculateFirstNightCharge(booking);
                BigDecimal serviceFee = calculateServiceFee(booking);
                cancellationFee = firstNightCharge.add(serviceFee);
                if (cancellationFee.compareTo(dueAmount) > 0) {
                    cancellationFee = dueAmount;
                }
                refundAmount = dueAmount.subtract(cancellationFee);
            } else {
                cancellationFee = dueAmount;
                refundAmount = BigDecimal.ZERO;
            }
        }

        refundAmount = refundAmount.max(BigDecimal.ZERO);
        cancellationFee = cancellationFee.max(BigDecimal.ZERO);

        if (invoice != null) {
            invoice.setCancellationFee(cancellationFee);
            invoice.setRefundableAmount(refundAmount);
            invoice.setRefundRequestedAt(cancelAt);
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                invoice.setStatus(InvoiceStatus.REFUND_PENDING);
                invoice.setRefundConfirmed(Boolean.FALSE);
                invoice.setRefundConfirmedAt(null);
            } else {
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setRefundConfirmed(Boolean.TRUE);
                invoice.setRefundConfirmedAt(cancelAt);
            }
            invoice.setUpdatedAt(cancelAt);
            invoiceRepo.save(invoice);
        }
    }

    private BigDecimal calculateFirstNightCharge(Booking booking) {
        if (booking == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = safe(booking.getTotalPrice());
        LocalDate startDate = booking.getStartDate();
        LocalDate endDate = booking.getEndDate();
        if (startDate == null || endDate == null) {
            return total;
        }
        long nights = ChronoUnit.DAYS.between(startDate, endDate);
        if (nights <= 0) {
            return total;
        }
        if (nights == 1) {
            return total;
        }
        return total.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateServiceFee(Booking booking) {
        if (booking == null) {
            return BigDecimal.ZERO;
        }
        Contract contract = booking.getContract();
        if (contract != null && contract.getServices() != null) {
            return contract.getServices().stream()
                    .filter(service -> service != null && Boolean.FALSE.equals(service.getIsIncluded()))
                    .map(service -> safe(service.getFee()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        Property property = booking.getProperty();
        if (property != null && property.getServices() != null) {
            return property.getServices().stream()
                    .filter(service -> service != null && Boolean.FALSE.equals(service.getIsIncluded()))
                    .map(service -> safe(service.getFee()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void broadcastPaymentStatus(Invoice invoice, Booking booking, PaymentWebhookPayload payload) {
        if (invoice == null) {
            return;
        }

        Map<String, Object> basePayload = new HashMap<>();
        basePayload.put("type", "PAYMENT_STATUS_CHANGED");
        basePayload.put("invoiceId", invoice.getInvoiceId());
        basePayload.put("invoiceNo", invoice.getInvoiceNo());
        basePayload.put("invoiceStatus", invoice.getStatus() != null ? invoice.getStatus().name() : null);
        basePayload.put("bookingId", booking != null ? booking.getBookingId() : null);
        basePayload.put("bookingStatus", booking != null && booking.getBookingStatus() != null
                ? booking.getBookingStatus().name() : null);
        basePayload.put("success", payload.isSuccess());
        basePayload.put("amount", payload.getAmount());
        basePayload.put("transactionId", payload.getTransactionId());
        basePayload.put("paidAt", invoice.getPaidAt() != null ? invoice.getPaidAt().toString() : null);
        basePayload.put("updatedAt", invoice.getUpdatedAt() != null ? invoice.getUpdatedAt().toString()
                : LocalDateTime.now().toString());

        messaging.convertAndSend(topicForInvoice(invoice.getInvoiceId()), basePayload);

        if (booking != null) {
            if (booking.getTenant() != null && booking.getTenant().getUserId() != null) {
                messaging.convertAndSend(
                        tenantPaymentTopic(booking.getTenant().getUserId()),
                        withAudience(basePayload, "TENANT")
                );
            }

            if (booking.getProperty() != null
                    && booking.getProperty().getLandlord() != null
                    && booking.getProperty().getLandlord().getUserId() != null) {
                messaging.convertAndSend(
                        landlordPaymentTopic(booking.getProperty().getLandlord().getUserId()),
                        withAudience(basePayload, "LANDLORD")
                );
            }
        }
    }

    private Map<String, Object> withAudience(Map<String, Object> base, String audience) {
        Map<String, Object> copy = new HashMap<>(base);
        copy.put("audience", audience);
        return copy;
    }

    private String topicForInvoice(String invoiceId) {
        return "/topic/payments/" + invoiceId;
    }

    private String tenantPaymentTopic(String tenantId) {
        return "/topic/tenants/" + tenantId + "/payments";
    }

    private String landlordPaymentTopic(String landlordId) {
        return "/topic/landlords/" + landlordId + "/payments";
    }
}