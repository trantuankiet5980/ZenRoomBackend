package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.BookingDto;
import vn.edu.iuh.fit.dtos.NotificationDto;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.entities.Notification;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.NotificationType;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.mappers.NotificationMapper;
import vn.edu.iuh.fit.repositories.NotificationRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.RealtimeNotificationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RealtimeNotificationServiceImpl implements RealtimeNotificationService {
    private static final Logger log = LoggerFactory.getLogger(RealtimeNotificationServiceImpl.class);
    private final SimpMessagingTemplate messaging;
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;

    private String topicUserNotifications(String userId) {
        return "/topic/user.notifications." + userId;
    }

    public void notifyAdminsPropertyCreated(PropertyDto p) {
        // A) Broadcast realtime cho admin FE
        var payload = Map.of(
                "type", "PROPERTY_CREATED",
                "propertyId", p.getPropertyId(),
                "title", p.getTitle(),
                "landlordName", p.getLandlord() != null ? p.getLandlord().getFullName() : null,
                "createdAt", LocalDateTime.now().toString()
        );
        messaging.convertAndSend("/topic/admin.notifications", payload);

        // B) (khuyến nghị) Lưu DB cho từng admin để có lịch sử
        List<User> admins = userRepository.findByRole_RoleName("admin");

        if (!admins.isEmpty()) {
            var now = LocalDateTime.now();
            var records = admins.stream().map(a ->
                    Notification.builder()
                            .user(a)
                            .title("Bài đăng mới")
                            .message(p.getTitle())
                            .type(NotificationType.SYSTEM)
                            .redirectUrl("/admin/properties/" + p.getPropertyId())
                            .isRead(false)
                            .createdAt(now)
                            .build()
            ).toList();
            notificationRepository.saveAll(records);
        }
    }

    @Override
    public void notifyAdminsPropertyUpdated(PropertyDto p) {
        // A) Broadcast lên topic admin
        var payload = Map.of(
                "type", "PROPERTY_UPDATED",
                "propertyId", p.getPropertyId(),
                "title", p.getTitle(),
                "landlordName", p.getLandlord() != null ? p.getLandlord().getFullName() : null,
                "createdAt", LocalDateTime.now().toString()
        );
        messaging.convertAndSend("/topic/admin.notifications", payload);

        // B) (tuỳ chọn) Lưu DB cho từng admin
        List<User> admins = userRepository.findAll() // hoặc findAllAdmins()
                .stream().filter(u -> /* isAdmin */ true).toList();

        if (!admins.isEmpty()) {
            var now = LocalDateTime.now();
            var records = admins.stream().map(a ->
                    Notification.builder()
                            .user(a)
                            .title("Bài đăng vừa được cập nhật")
                            .message(p.getTitle())
                            .type(NotificationType.SYSTEM)
                            .redirectUrl("/admin/properties/" + p.getPropertyId())
                            .isRead(false)
                            .createdAt(now)
                            .build()
            ).toList();
            notificationRepository.saveAll(records);
        }
    }

    @Override
    public void notifyAdminsPropertyStatusChanged(PropertyDto p, PostStatus status, String rejectedReason) {
        var now = java.time.LocalDateTime.now();

        // ===== 1) Payload chung =====
        var payload = new java.util.HashMap<String, Object>();
        payload.put("type", "PROPERTY_STATUS_CHANGED");
        payload.put("propertyId", p.getPropertyId());
        payload.put("title", p.getTitle());
        payload.put("status", status.name());
        if (rejectedReason != null && !rejectedReason.isBlank()) {
            payload.put("rejectedReason", rejectedReason);
        }
        payload.put("createdAt", now.toString());

        // =====Gửi  LANDLORD (DB + WS /user/queue/notifications) =====
        if (p.getLandlord() != null && p.getLandlord().getUserId() != null) {
            var landlordId = p.getLandlord().getUserId();

            var landlordTitle = switch (status) {
                case APPROVED -> "Bài đăng của bạn đã được DUYỆT";
                case REJECTED -> "Bài đăng của bạn BỊ TỪ CHỐI";
                default -> "Bài đăng của bạn ĐANG CHỜ DUYỆT";
            };
            var landlordMsg = (rejectedReason != null && !rejectedReason.isBlank())
                    ? (p.getTitle() + " | Lý do: " + rejectedReason)
                    : p.getTitle();

            // Lưu 1 bản ghi cho landlord
            notificationRepository.save(
                    vn.edu.iuh.fit.entities.Notification.builder()
                            .user(userRepository.getReferenceById(landlordId))
                            .title(landlordTitle)
                            .message(landlordMsg)
                            .type(NotificationType.SYSTEM)
                            .redirectUrl("/landlord/properties/" + p.getPropertyId())
                            .isRead(false)
                            .createdAt(now)
                            .build()
            );

            // Push realtime tới landlord (cần setUserDestinationPrefix("/user") + Interceptor gán Principal = userId)
            var payloadLandlord = new java.util.HashMap<String, Object>(payload);
            payloadLandlord.put("audience", "LANDLORD");
            log.info("Sending WS notification to landlord {}: {}", landlordId, payloadLandlord);
            messaging.convertAndSendToUser(landlordId, "/queue/notifications", payloadLandlord);
        }
    }

    @Override
    public void notifyTenantBookingApproved(BookingDto booking) {
        if (booking == null || booking.getTenant() == null || booking.getTenant().getUserId() == null) {
            return;
        }

        String tenantId = booking.getTenant().getUserId();
        userRepository.findById(tenantId).ifPresent(tenant -> {
            String tenantTitle = "Đặt phòng của bạn đã được duyệt";
            String tenantMessage = booking.getProperty() != null && booking.getProperty().getTitle() != null
                    ? booking.getProperty().getTitle()
                    : "Đặt phòng #" + booking.getBookingId();

            createAndPush(
                    tenant,
                    tenantTitle,
                    tenantMessage,
                    NotificationType.BOOKING,
                    "/tenant/bookings/" + booking.getBookingId()
            );

            log.info("Sent booking approved notification to tenant {} for booking {}", tenantId, booking.getBookingId());
        });
    }

    @Override
    public void notifyLandlordBookingCreated(BookingDto booking) {
        if (booking == null
                || booking.getProperty() == null
                || booking.getProperty().getLandlord() == null
                || booking.getProperty().getLandlord().getUserId() == null) {
            return;
        }

        var landlordId = booking.getProperty().getLandlord().getUserId();
        userRepository.findById(landlordId).ifPresent(landlord -> {
            String propertyTitle = booking.getProperty().getTitle() != null
                    ? booking.getProperty().getTitle()
                    : "đặt phòng";
            String tenantName = booking.getTenant() != null && booking.getTenant().getFullName() != null
                    ? booking.getTenant().getFullName()
                    : "Một khách thuê";
            String message = tenantName + " đã đặt " + propertyTitle;

            createAndPush(
                    landlord,
                    "Có đặt phòng mới",
                    message,
                    NotificationType.BOOKING,
                    "/landlord/bookings/" + booking.getBookingId()
            );

            log.info("Sent new booking notification to landlord {} for booking {}", landlordId, booking.getBookingId());
        });
    }

    @Override
    public void notifyPaymentStatusChanged(BookingDto booking, Invoice invoice, boolean success) {
        if (booking == null) {
            return;
        }

        String propertyTitle = booking.getProperty() != null && booking.getProperty().getTitle() != null
                ? booking.getProperty().getTitle()
                : "đặt phòng";
        String invoiceNo = invoice != null ? invoice.getInvoiceNo() : null;

        if (booking.getTenant() != null && booking.getTenant().getUserId() != null) {
            String tenantId = booking.getTenant().getUserId();
            userRepository.findById(tenantId).ifPresent(tenant -> {
                String title = success ? "Thanh toán thành công" : "Thanh toán thất bại";
                String message;
                if (success) {
                    message = "Bạn đã thanh toán thành công cho " + propertyTitle
                            + (invoiceNo != null ? " (" + invoiceNo + ")" : "");
                } else {
                    message = "Thanh toán cho " + propertyTitle + " không thành công. Vui lòng thử lại.";
                }

                createAndPush(
                        tenant,
                        title,
                        message,
                        NotificationType.PAYMENT,
                        "/tenant/bookings/" + booking.getBookingId()
                );
            });
        }

        if (!success) {
            return;
        }

        if (booking.getProperty() != null
                && booking.getProperty().getLandlord() != null
                && booking.getProperty().getLandlord().getUserId() != null) {
            String landlordId = booking.getProperty().getLandlord().getUserId();
            userRepository.findById(landlordId).ifPresent(landlord -> {
                String tenantName = booking.getTenant() != null && booking.getTenant().getFullName() != null
                        ? booking.getTenant().getFullName()
                        : "Khách thuê";
                String message = tenantName + " đã hoàn tất thanh toán cho " + propertyTitle
                        + (invoiceNo != null ? " (" + invoiceNo + ")" : "");

                createAndPush(
                        landlord,
                        "Đặt phòng đã được thanh toán",
                        message,
                        NotificationType.PAYMENT,
                        "/landlord/bookings/" + booking.getBookingId()
                );
            });
        }
    }

    @Override
    public void notifyBookingCheckedIn(BookingDto booking) {
        if (booking == null) {
            return;
        }

        String bookingId = booking.getBookingId();
        String propertyTitle = booking.getProperty() != null && booking.getProperty().getTitle() != null
                ? booking.getProperty().getTitle()
                : "đặt phòng";

        if (booking.getTenant() != null && booking.getTenant().getUserId() != null) {
            String tenantId = booking.getTenant().getUserId();
            userRepository.findById(tenantId).ifPresent(tenant -> {
                String message = "Bạn đã check-in thành công vào " + propertyTitle + ".";
                createAndPush(
                        tenant,
                        "Check-in thành công",
                        message,
                        NotificationType.BOOKING,
                        "/tenant/bookings/" + bookingId
                );
            });
        }

        if (booking.getProperty() != null
                && booking.getProperty().getLandlord() != null
                && booking.getProperty().getLandlord().getUserId() != null) {
            String landlordId = booking.getProperty().getLandlord().getUserId();
            String tenantName = booking.getTenant() != null && booking.getTenant().getFullName() != null
                    ? booking.getTenant().getFullName()
                    : "Khách thuê";
            userRepository.findById(landlordId).ifPresent(landlord -> {
                String message = tenantName + " đã check-in vào " + propertyTitle + ".";
                createAndPush(
                        landlord,
                        "Khách thuê đã check-in",
                        message,
                        NotificationType.BOOKING,
                        "/landlord/bookings/" + bookingId
                );
            });
        }
    }

    @Override
    public void notifyBookingCheckedOut(BookingDto booking) {
        if (booking == null) {
            return;
        }

        String bookingId = booking.getBookingId();
        String propertyTitle = booking.getProperty() != null && booking.getProperty().getTitle() != null
                ? booking.getProperty().getTitle()
                : "đặt phòng";

        if (booking.getTenant() != null && booking.getTenant().getUserId() != null) {
            String tenantId = booking.getTenant().getUserId();
            userRepository.findById(tenantId).ifPresent(tenant -> {
                String message = "Bạn đã check-out khỏi " + propertyTitle + ".";
                createAndPush(
                        tenant,
                        "Check-out thành công",
                        message,
                        NotificationType.BOOKING,
                        "/tenant/bookings/" + bookingId
                );
            });
        }

        if (booking.getProperty() != null
                && booking.getProperty().getLandlord() != null
                && booking.getProperty().getLandlord().getUserId() != null) {
            String landlordId = booking.getProperty().getLandlord().getUserId();
            String tenantName = booking.getTenant() != null && booking.getTenant().getFullName() != null
                    ? booking.getTenant().getFullName()
                    : "Khách thuê";
            userRepository.findById(landlordId).ifPresent(landlord -> {
                String message = tenantName + " đã check-out khỏi " + propertyTitle + ".";
                createAndPush(
                        landlord,
                        "Khách thuê đã check-out",
                        message,
                        NotificationType.BOOKING,
                        "/landlord/bookings/" + bookingId
                );
            });
        }
    }

    @Override
    public void notifyRefundProcessed(BookingDto booking, Invoice invoice) {
        if (invoice == null) {
            return;
        }

        String bookingId = booking != null ? booking.getBookingId() : null;
        String propertyTitle = booking != null
                && booking.getProperty() != null
                && booking.getProperty().getTitle() != null
                ? booking.getProperty().getTitle()
                : "đặt phòng";
        BigDecimal refundAmount = invoice.getRefundableAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            refundAmount = invoice.getDueAmount();
        }
        String refundAmountText = refundAmount != null
                ? refundAmount.stripTrailingZeros().toPlainString()
                : "0";

        if (booking != null && booking.getTenant() != null && booking.getTenant().getUserId() != null) {
            String tenantId = booking.getTenant().getUserId();
            userRepository.findById(tenantId).ifPresent(tenant -> {
                String message = "Yêu cầu hoàn tiền cho " + propertyTitle
                        + " đã được xử lý. Số tiền hoàn: " + refundAmountText + ".";
                createAndPush(
                        tenant,
                        "Hoàn tiền đã được xử lý",
                        message,
                        NotificationType.PAYMENT,
                        "/tenant/bookings/" + bookingId
                );
            });
        }

        if (booking != null
                && booking.getProperty() != null
                && booking.getProperty().getLandlord() != null
                && booking.getProperty().getLandlord().getUserId() != null) {
            String landlordId = booking.getProperty().getLandlord().getUserId();
            String tenantName = booking.getTenant() != null && booking.getTenant().getFullName() != null
                    ? booking.getTenant().getFullName()
                    : "Khách thuê";
            userRepository.findById(landlordId).ifPresent(landlord -> {
                String message = "Đã hoàn " + refundAmountText + " cho " + tenantName
                        + " trong " + propertyTitle + ".";
                createAndPush(
                        landlord,
                        "Đơn đặt phòng đã hoàn tiền",
                        message,
                        NotificationType.PAYMENT,
                        "/landlord/bookings/" + bookingId
                );
            });
        }

        List<User> admins = userRepository.findByRole_RoleName("admin");
        if (!admins.isEmpty()) {
            var payload = new HashMap<String, Object>();
            payload.put("type", "REFUND_CONFIRMED");
            payload.put("invoiceId", invoice.getInvoiceId());
            payload.put("invoiceNo", invoice.getInvoiceNo());
            payload.put("bookingId", bookingId);
            payload.put("amount", refundAmountText);
            payload.put("createdAt", LocalDateTime.now().toString());

            messaging.convertAndSend("/topic/admin.notifications", payload);

            String adminMessage = "Hoàn tiền cho booking "
                    + (bookingId != null ? bookingId : "")
                    + " đã được xác nhận. Số tiền: " + refundAmountText + ".";
            for (User admin : admins) {
                createAndPush(
                        admin,
                        "Hoàn tiền đã được xác nhận",
                        adminMessage,
                        NotificationType.PAYMENT,
                        "/admin/invoices/" + invoice.getInvoiceId()
                );
            }
        }
    }

    @Override
    public NotificationDto createAndPush(User target, String title, String message,
                                         NotificationType type, String redirectUrl) {
        Notification entity = new Notification();
        entity.setNotificationId(UUID.randomUUID().toString());
        entity.setUser(target);
        entity.setTitle(title);
        entity.setMessage(message);
        entity.setType(type);
        entity.setRedirectUrl(redirectUrl);
        entity.setIsRead(false);
        entity.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(entity);
        NotificationDto dto = notificationMapper.toDto(saved);

//        messaging.convertAndSendToUser(
//                target.getUserId(),            // Principal.getName() == userId
//                "/queue/notifications",
//                dto
//        );
        messaging.convertAndSend(
                topicUserNotifications(target.getUserId()),
                dto
        );
        return dto;
    }
}
