package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.BookingDto;
import vn.edu.iuh.fit.dtos.requests.BookingCreateRequest;
import vn.edu.iuh.fit.dtos.requests.PaymentWebhookPayload;
import vn.edu.iuh.fit.entities.Booking;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.BookingStatus;
import vn.edu.iuh.fit.entities.enums.InvoiceStatus;
import vn.edu.iuh.fit.entities.enums.NotificationType;
import vn.edu.iuh.fit.mappers.BookingMapper;
import vn.edu.iuh.fit.mappers.InvoiceMapper;
import vn.edu.iuh.fit.payments.PaymentGateway;
import vn.edu.iuh.fit.repositories.BookingRepository;
import vn.edu.iuh.fit.repositories.InvoiceRepository;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.BookingService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepo;
    private final PropertyRepository propertyRepo;
    private final UserRepository userRepo;
    private final InvoiceRepository invoiceRepo;
    private final BookingMapper bookingMapper;   // bạn đã có
    private final InvoiceMapper invoiceMapper;   // nếu cần
    private final PaymentGateway paymentGateway; // triển khai thực tế
    private final RealtimeNotificationServiceImpl notificationService; // đã có createAndPush

    @Transactional
    @Override
    public BookingDto createDaily(String tenantId, BookingCreateRequest req) {
        if (req.getPropertyId() == null || req.getCheckInAt() == null || req.getCheckOutAt() == null)
            throw new IllegalArgumentException("propertyId, checkInAt, checkOutAt are required");
        if (!req.getCheckOutAt().isAfter(req.getCheckInAt()))
            throw new IllegalArgumentException("checkOutAt must be after checkInAt");

        Property property = propertyRepo.findById(req.getPropertyId()).orElseThrow();
        User tenant = userRepo.findById(tenantId).orElseThrow();

        if (bookingRepo.existsOverlap(property.getPropertyId(), req.getCheckInAt(), req.getCheckOutAt()))
            throw new IllegalStateException("Property not available in selected dates");

        BigDecimal pricePerDay = property.getPrice();
        long days = Duration.between(req.getCheckInAt(), req.getCheckOutAt()).toDays();
        if (days <= 0) throw new IllegalArgumentException("At least 1 day");
        BigDecimal total = pricePerDay.multiply(BigDecimal.valueOf(days));

        Booking b = new Booking();
        b.setBookingId(UUID.randomUUID().toString());
        b.setProperty(property);
        b.setTenant(tenant);
        b.setStartDate(req.getCheckInAt());
        b.setEndDate(req.getCheckOutAt());
        b.setNote(req.getNote());
        b.setTotalPrice(total);
        b.setBookingStatus(BookingStatus.PENDING);
        b.setCreatedAt(LocalDateTime.now());

        Booking saved = bookingRepo.save(b);

//        notificationService.createAndPush(
//                property.getLandlord(),
//                "Yêu cầu đặt phòng mới",
//                tenant.getFullName() + " muốn đặt phòng " + property.getTitle(),
//                NotificationType.BOOKING,
//                "/landlord/bookings/" + saved.getBookingId()
//        );

        return bookingMapper.toDto(saved);
    }

    @Override
    public BookingDto approve(String bookingId, String landlordId) {
        Booking b = bookingRepo.findById(bookingId).orElseThrow();
        if (!b.getProperty().getLandlord().getUserId().equals(landlordId))
            throw new SecurityException("Only landlord can approve");
        if (b.getBookingStatus() != BookingStatus.PENDING)
            throw new IllegalStateException("Booking not in PENDING");

        b.setBookingStatus(BookingStatus.APPROVED);
        b.setUpdatedAt(LocalDateTime.now());

        BigDecimal total = b.getTotalPrice();
        BigDecimal deposit = total.multiply(new BigDecimal("0.5")).setScale(2, BigDecimal.ROUND_HALF_UP);

        Invoice inv = invoiceRepo.findByBooking_BookingId(bookingId).orElseGet(() -> {
            Invoice i = new Invoice();
            i.setInvoiceId(UUID.randomUUID().toString());
            i.setBooking(b);
            i.setInvoiceNo(genInvoiceNo()); // bắt buộc vì NOT NULL + UNIQUE
            i.setCreatedAt(LocalDateTime.now());
            return i;
        });
        inv.setStatus(InvoiceStatus.ISSUED);
        inv.setIssuedAt(LocalDateTime.now());
        inv.setDueAt(LocalDateTime.now().plusHours(24)); // hạn thanh toán cọc: 24h
        // HÓA ĐƠN TỔNG cho booking:
        inv.setTotal(total);
        inv.setSubtotal(total);
        if (inv.getTax() == null) inv.setTax(BigDecimal.ZERO);
        if (inv.getDiscount() == null) inv.setDiscount(BigDecimal.ZERO);
        // BAN ĐẦU chỉ thu 50%:
        inv.setDueAmount(deposit);
        inv.setUpdatedAt(LocalDateTime.now());
        invoiceRepo.save(inv);

        String payUrl = paymentGateway.createPayment(
                inv.getInvoiceId(),                // amount tham số long không còn cần thiết – nhưng interface vẫn ok, có thể truyền deposit.longValue()
                deposit.longValue(),               // nếu muốn sửa interface, đổi sang BigDecimal
                "Cọc 50% booking " + b.getBookingId(),
                "https://yourapp/booking/return",
                "https://yourapp/api/v1/payments/webhook"
        );

        b.setPaymentUrl(payUrl);

//        realtime
//        notificationService.createAndPush(
//                b.getTenant(), "Yêu cầu được duyệt",
//                "Vui lòng thanh toán 50% để giữ chỗ.", NotificationType.PAYMENT,
//                "/bookings/" + b.getBookingId()
//        );
        bookingRepo.save(b);
        return bookingMapper.toDto(b);
    }

    @Transactional
    @Override
    public BookingDto reject(String bookingId, String landlordId) {
        Booking b = bookingRepo.findById(bookingId).orElseThrow();
        if (!b.getProperty().getLandlord().getUserId().equals(landlordId))
            throw new SecurityException("Only landlord can reject");
        if (b.getBookingStatus() != BookingStatus.PENDING)
            throw new IllegalStateException("Booking not in PENDING");

        b.setUpdatedAt(LocalDateTime.now());
        b.setBookingStatus(BookingStatus.REJECTED);

//        thong bao realtime
//        notificationService.createAndPush(
//                b.getTenant(), "Yêu cầu bị từ chối",
//                "Chủ nhà đã từ chối yêu cầu đặt của bạn.", NotificationType.BOOKING,
//                "/bookings/" + b.getBookingId()
//        );
        return bookingMapper.toDto(b);
    }

    @Override
    public BookingDto cancel(String bookingId, String tenantId) {
        Booking b = bookingRepo.findById(bookingId).orElseThrow();
        if (!b.getTenant().getUserId().equals(tenantId))
            throw new SecurityException("Only tenant can cancel");
        if (EnumSet.of(BookingStatus.REJECTED, BookingStatus.CANCELLED, BookingStatus.COMPLETED).contains(b.getBookingStatus()))
            throw new IllegalStateException("Booking cannot be cancelled");

        b.setUpdatedAt(LocalDateTime.now());
        b.setBookingStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepo.save(b);

//        notificationService.createAndPush(
//                b.getProperty().getLandlord(), "Khách hủy đặt phòng",
//                b.getTenant().getFullName() + " đã hủy booking " + b.getBookingId(),
//                NotificationType.BOOKING, "/landlord/bookings/" + b.getBookingId()
//        );
        return bookingMapper.toDto(saved);
    }

    @Override
    public BookingDto checkIn(String bookingId, String tenantId) {
        Booking b = bookingRepo.findById(bookingId).orElseThrow();
        if (!b.getTenant().getUserId().equals(tenantId))
            throw new SecurityException("Only tenant can check-in");
        if (b.getBookingStatus() != BookingStatus.APPROVED)
            throw new IllegalStateException("Booking not approved");

        // Cọc đã trả: hoặc kiểm tra invoice dueAmount đã giảm tương ứng
        Invoice inv = invoiceRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new IllegalStateException("Invoice not found"));
        BigDecimal total = inv.getTotal();
        BigDecimal due = inv.getDueAmount();
        if (due.compareTo(total.multiply(new BigDecimal("0.5"))) > 0) {
            throw new IllegalStateException("Deposit not paid");
        }

        b.setUpdatedAt(LocalDateTime.now());
        b.setBookingStatus(BookingStatus.CHECKED_IN);
        return bookingMapper.toDto(bookingRepo.save(b));
    }

    @Transactional
    @Override
    public BookingDto checkOut(String bookingId, String tenantId) {
        Booking b = bookingRepo.findById(bookingId).orElseThrow();
        if (!b.getTenant().getUserId().equals(tenantId))
            throw new SecurityException("Only tenant can check-out");
        if (b.getBookingStatus() != BookingStatus.CHECKED_IN)
            throw new IllegalStateException("Booking is not in CHECKED_IN status");

        Invoice inv = invoiceRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new IllegalStateException("Invoice not found"));

        BigDecimal remaining = inv.getDueAmount(); // phần còn lại cần thu
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            // đã thanh toán đủ
            b.setBookingStatus(BookingStatus.COMPLETED);
            b.setUpdatedAt(LocalDateTime.now());
            return bookingMapper.toDto(bookingRepo.save(b));
        }

        // Cập nhật hạn thanh toán phần còn lại và phát link
        inv.setStatus(InvoiceStatus.ISSUED);
        inv.setDueAt(LocalDateTime.now().plusHours(2));
        inv.setUpdatedAt(LocalDateTime.now());
        invoiceRepo.save(inv);

        String payUrl = paymentGateway.createPayment(
                inv.getInvoiceId(),
                remaining.longValue(),
                "Thanh toán phần còn lại booking " + b.getBookingId(),
                "https://yourapp/booking/return",
                "https://yourapp/api/v1/payments/webhook"
        );
        b.setPaymentUrl(payUrl);
        b.setUpdatedAt(LocalDateTime.now());
        return bookingMapper.toDto(bookingRepo.save(b));
    }

    @Override
    public BookingDto getOne(String bookingId, String userId) {
        Booking b = bookingRepo.findById(bookingId).orElseThrow();

        // chỉ tenant hoặc landlord của booking mới được xem
        boolean isTenant = b.getTenant() != null && b.getTenant().getUserId().equals(userId);
        boolean isLandlord = b.getProperty() != null
                && b.getProperty().getLandlord() != null
                && b.getProperty().getLandlord().getUserId().equals(userId);

        if (!isTenant && !isLandlord) {
            throw new SecurityException("Not allowed to view this booking");
        }

        return bookingMapper.toDto(b);
    }

    @Override
    public void handlePaymentWebhook(PaymentWebhookPayload payload) {
        Invoice inv = invoiceRepo.findById(payload.getInvoiceId()).orElseThrow();

        // Tính số tiền ghi nhận: nếu gateway không gửi amount,
        // cho fake = toàn bộ due hiện tại (tức trả đủ phần đã yêu cầu)
        BigDecimal paidNow;
        if (payload.getAmount() != 0) {
            paidNow = new BigDecimal(payload.getAmount());
        } else {
            paidNow = inv.getDueAmount();
        }

        if (payload.isSuccess()) {
            BigDecimal newDue = inv.getDueAmount().subtract(paidNow);
            if (newDue.compareTo(BigDecimal.ZERO) < 0) newDue = BigDecimal.ZERO;

            inv.setDueAmount(newDue);
            inv.setPaidAt(LocalDateTime.now());
            inv.setStatus(newDue.compareTo(BigDecimal.ZERO) == 0 ? InvoiceStatus.PAID : InvoiceStatus.ISSUED);
        } else {
            // thất bại: giữ nguyên dueAmount; có thể log, set status ISSUED
            inv.setStatus(InvoiceStatus.ISSUED);
        }
        inv.setUpdatedAt(LocalDateTime.now());
        invoiceRepo.save(inv);

        Booking b = inv.getBooking();
        // Nếu đã check-in và đã thanh toán đủ, (tuỳ) chỉ hoàn tất khi đã tới hoặc qua end_date
        if (payload.isSuccess()
                && b.getBookingStatus() == BookingStatus.CHECKED_IN
                && inv.getDueAmount().compareTo(BigDecimal.ZERO) == 0) {
            // Nếu endDate là DATE thì so theo ngày; nếu LocalDateTime, giữ như dưới:
            if (!LocalDateTime.now().isBefore(b.getEndDate())) {
                b.setBookingStatus(BookingStatus.COMPLETED);
                b.setUpdatedAt(LocalDateTime.now());
                bookingRepo.save(b);
            }
        }
    }

    private String genInvoiceNo() {
        // Ví dụ: INV-20250922-ABCDEFG (ngẫu nhiên 7 ký tự)
        String date = java.time.LocalDate.now().toString().replace("-", "");
        String rand = java.util.UUID.randomUUID().toString().substring(0, 7).toUpperCase();
        return "INV-" + date + "-" + rand;
    }
}
