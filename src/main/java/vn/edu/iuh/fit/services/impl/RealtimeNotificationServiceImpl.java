package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.BookingDto;
import vn.edu.iuh.fit.dtos.NotificationDto;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.entities.Notification;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.NotificationType;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.mappers.NotificationMapper;
import vn.edu.iuh.fit.repositories.NotificationRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.RealtimeNotificationService;

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
                case APPROVED -> "Tin của bạn đã được DUYỆT";
                case REJECTED -> "Tin của bạn BỊ TỪ CHỐI";
                default -> "Tin của bạn ĐANG CHỜ DUYỆT";
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
        var now = LocalDateTime.now();

        // Prepare WebSocket payload
        var payload = new HashMap<String, Object>();
        payload.put("type", "BOOKING_APPROVED");
        payload.put("bookingId", booking.getBookingId());
        payload.put("propertyTitle", booking.getProperty() != null ? booking.getProperty().getTitle() : null);
        payload.put("status", booking.getBookingStatus() != null ? booking.getBookingStatus().name() : null);
        payload.put("createdAt", now.toString());

        // Notify tenant
        if (booking.getTenant() != null && booking.getTenant().getUserId() != null) {
            var tenantId = booking.getTenant().getUserId();
            var tenantTitle = "Đặt phòng của bạn đã được duyệt";
            var tenantMessage = booking.getProperty() != null ? booking.getProperty().getTitle() : "Đặt phòng #" + booking.getBookingId();

            // Save notification to database
            notificationRepository.save(
                    Notification.builder()
                            .user(userRepository.getReferenceById(tenantId))
                            .title(tenantTitle)
                            .message(tenantMessage)
                            .type(NotificationType.BOOKING)
                            .redirectUrl("/tenant/bookings/" + booking.getBookingId())
                            .isRead(false)
                            .createdAt(now)
                            .build()
            );

            // Send WebSocket notification to tenant
            var payloadTenant = new HashMap<String, Object>(payload);
            payloadTenant.put("audience", "TENANT");
            log.info("Sending WS notification to tenant {}: {}", tenantId, payloadTenant);
            messaging.convertAndSendToUser(tenantId, "/queue/notifications", payloadTenant);
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

        messaging.convertAndSendToUser(
                target.getUserId(),            // Principal.getName() == userId
                "/queue/notifications",
                dto
        );
        return dto;
    }
}
