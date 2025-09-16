// vn/edu/iuh/fit/services/RealtimeNotificationService.java
package vn.edu.iuh.fit.services;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.entities.Notification;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.NotificationType;
import vn.edu.iuh.fit.repositories.NotificationRepository;
import vn.edu.iuh.fit.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RealtimeNotificationServiceImpl implements RealtimeNotificationService {
    private final SimpMessagingTemplate messaging;
    private final NotificationRepository notificationRepository;
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
}
