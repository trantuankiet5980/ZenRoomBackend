package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
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

        return bookingMapper.toDto(savedBooking);
    }

    @Override
    public BookingDto approve(String bookingId, String landlordId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        if (booking.getProperty() == null || booking.getProperty().getLandlord() == null
                || !booking.getProperty().getLandlord().getUserId().equals(landlordId)) {
            throw new SecurityException("Only landlord can approve this booking");
        }
        if (booking.getBookingStatus() != BookingStatus.AWAITING_LANDLORD_APPROVAL) {
            throw new IllegalStateException("Booking is not waiting for landlord approval");
        }

        Invoice invoice = invoiceRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new IllegalStateException("Invoice not found"));
        BigDecimal due = invoice.getDueAmount() == null ? BigDecimal.ZERO : invoice.getDueAmount();
        if (due.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Deposit not paid");
        }

        booking.setBookingStatus(BookingStatus.APPROVED);
        booking.setUpdatedAt(LocalDateTime.now());
        Booking saved = bookingRepo.save(booking);

        contractRepo.findByBooking_BookingId(bookingId).ifPresent(contract -> {
            contract.setContractStatus(ContractStatus.ACTIVE);
            contract.setUpdatedAt(LocalDateTime.now());
            contractRepo.save(contract);
        });
        return bookingMapper.toDto(saved);
    }

    @Override
    public BookingDto cancel(String bookingId, String tenantId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        if (!booking.getTenant().getUserId().equals(tenantId)) {
            throw new SecurityException("Only tenant can cancel");
        }
        if (EnumSet.of(BookingStatus.CANCELLED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED, BookingStatus.APPROVED)
                .contains(booking.getBookingStatus())) {
            throw new IllegalStateException("Booking cannot be cancelled at this stage");
        }
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepo.save(booking);

        contractRepo.findByBooking_BookingId(bookingId).ifPresent(contract -> {
            contract.setContractStatus(ContractStatus.CANCELLED);
            contract.setUpdatedAt(LocalDateTime.now());
            contractRepo.save(contract);
        });

        invoiceRepo.findByBooking_BookingId(bookingId).ifPresent(inv -> {
            inv.setStatus(InvoiceStatus.VOID);
            inv.setUpdatedAt(LocalDateTime.now());
            invoiceRepo.save(inv);
        });

        return bookingMapper.toDto(booking);
    }

    @Override
    public BookingDto checkIn(String bookingId, String tenantId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        if (!booking.getTenant().getUserId().equals(tenantId)) {
            throw new SecurityException("Only tenant can check-in");
        }
        if (booking.getBookingStatus() != BookingStatus.APPROVED) {
            throw new IllegalStateException("Booking has not been approved");
        }

        Invoice invoice = invoiceRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new IllegalStateException("Invoice not found"));
        BigDecimal due = invoice.getDueAmount() == null ? BigDecimal.ZERO : invoice.getDueAmount();
        if (due.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Booking still has outstanding balance");
        }

        booking.setBookingStatus(BookingStatus.CHECKED_IN);
        booking.setUpdatedAt(LocalDateTime.now());
        return bookingMapper.toDto(bookingRepo.save(booking));
    }

    @Transactional
    @Override
    public BookingDto checkOut(String bookingId, String tenantId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        if (!booking.getTenant().getUserId().equals(tenantId)) {
            throw new SecurityException("Only tenant can check-out");
        }
        if (booking.getBookingStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Booking is not in CHECKED_IN status");
        }
        Invoice invoice = invoiceRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new IllegalStateException("Invoice not found"));
        BigDecimal due = invoice.getDueAmount() == null ? BigDecimal.ZERO : invoice.getDueAmount();
        if (due.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Booking still has outstanding balance");
        }

        booking.setBookingStatus(BookingStatus.COMPLETED);
        booking.setUpdatedAt(LocalDateTime.now());
        return bookingMapper.toDto(bookingRepo.save(booking));
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
        Invoice invoice = invoiceRepo.findById(payload.getInvoiceId()).orElseThrow();

        BigDecimal paidNow = payload.getAmount() != 0
                ? new BigDecimal(payload.getAmount())
                : invoice.getDueAmount();

        if (payload.isSuccess()) {
            BigDecimal newDue = invoice.getDueAmount().subtract(paidNow);
            if (newDue.compareTo(BigDecimal.ZERO) < 0) {
                newDue = BigDecimal.ZERO;
            }
            invoice.setDueAmount(newDue);
            invoice.setPaidAt(LocalDateTime.now());
            invoice.setStatus(newDue.compareTo(BigDecimal.ZERO) == 0 ? InvoiceStatus.PAID : InvoiceStatus.ISSUED);
        } else {
            invoice.setStatus(InvoiceStatus.ISSUED);
        }
        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepo.save(invoice);

        Booking booking = invoice.getBooking();
        if (payload.isSuccess() && invoice.getDueAmount().compareTo(BigDecimal.ZERO) == 0) {
            booking.setBookingStatus(BookingStatus.AWAITING_LANDLORD_APPROVAL);
            booking.setPaymentUrl(null);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepo.save(booking);
            contractRepo.findByBooking_BookingId(booking.getBookingId()).ifPresent(contract -> {
                if (contract.getContractStatus() == ContractStatus.CANCELLED) {
                    contract.setContractStatus(ContractStatus.PENDING_REVIEW);
                }
                contract.setUpdatedAt(LocalDateTime.now());
                contractRepo.save(contract);
            });
        }
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
}
