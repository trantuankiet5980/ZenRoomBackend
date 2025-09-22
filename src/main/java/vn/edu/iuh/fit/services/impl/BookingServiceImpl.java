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

        BigDecimal deposit = b.getTotalPrice().multiply(new BigDecimal("0.5"));

        Invoice inv = new Invoice();
        inv.setInvoiceId(UUID.randomUUID().toString());
        inv.setInvoiceNo(genInvoiceNo());
        inv.setBooking(b);
        inv.setTotal(deposit);
        inv.setDueAmount(deposit);
        inv.setStatus(InvoiceStatus.ISSUED);
        inv.setCreatedAt(LocalDateTime.now());
        inv.setDueAt(LocalDateTime.now().plusDays(3));
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

        boolean paid = !invoiceRepo.findPaidByBooking(bookingId).isEmpty();
        if (!paid) throw new IllegalStateException("Deposit not paid");

//        if (LocalDateTime.now().isBefore(b.getStartDate().minusHours(2)))
//            throw new IllegalStateException("Too early to check-in");

        b.setUpdatedAt(LocalDateTime.now());
        b.setBookingStatus(BookingStatus.CHECKED_IN);

//        notificationService.createAndPush(
//                b.getProperty().getLandlord(), "Khách đã check-in",
//                b.getTenant().getFullName() + " đã nhận phòng.", NotificationType.BOOKING,
//                "/landlord/bookings/" + b.getBookingId()
//        );

        Booking saved = bookingRepo.save(b);

        return bookingMapper.toDto(saved);
    }

    @Override
    public void handlePaymentWebhook(PaymentWebhookPayload payload) {
        Invoice inv = invoiceRepo.findById(payload.getInvoiceId()).orElseThrow();
        if (inv.getStatus() == InvoiceStatus.PAID) return; // idempotent

        inv.setStatus(payload.isSuccess() ? InvoiceStatus.PAID : InvoiceStatus.VOID);
        invoiceRepo.save(inv);

        Booking b = inv.getBooking();
//        notificationService.createAndPush(
//                b.getTenant(),
//                payload.isSuccess() ? "Thanh toán thành công" : "Thanh toán thất bại",
//                "Hóa đơn: " + inv.getInvoiceId(), NotificationType.PAYMENT,
//                "/bookings/" + b.getBookingId()
//        );
    }

    private String genInvoiceNo() {
        // Ví dụ: INV-20250922-ABCDEFG (ngẫu nhiên 7 ký tự)
        String date = java.time.LocalDate.now().toString().replace("-", "");
        String rand = java.util.UUID.randomUUID().toString().substring(0, 7).toUpperCase();
        return "INV-" + date + "-" + rand;
    }
}
