package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.NotificationDto;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.NotificationType;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.RealtimeNotificationService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevSocketTestController {
    private final SimpMessagingTemplate messaging;
    private final RealtimeNotificationService notificationService;
    private final UserRepository userRepository;

    @PostMapping("/ping-notif")
    public Map<String, Object> ping() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "PROPERTY_CREATED");
        payload.put("title", "Test realtime 🔔");
        payload.put("message", "Payload thử nghiệm");
        payload.put("createdAt", LocalDateTime.now().toString());

        messaging.convertAndSend("/topic/admin.notifications", payload);
        return payload;
    }
    @PostMapping("/ping-user/{uid}")
    public Map<String,Object> pingUser(@PathVariable String uid) {
        var payload = Map.of("type","PROPERTY_STATUS_CHANGED","title","Ping test","status","APPROVED");
        messaging.convertAndSendToUser(uid, "/queue/notifications", payload);
        return Map.of("ok",true);
    }

    /** Bắn notify tới 1 user cụ thể (by userId) */
    @PostMapping("/user/{userId}")
    public NotificationDto pushToUser(
            @PathVariable String userId,
            @RequestBody NotificationReq req
    ) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return notificationService.createAndPush(
                target,
                req.title(),
                req.message(),
                req.type() != null ? req.type() : NotificationType.SYSTEM,
                req.redirectUrl()
        );
    }

    /** Bắn notify tới toàn bộ admin (role ADMIN) */
    @PostMapping("/admins")
    public int pushToAdmins(@RequestBody NotificationReq req) {
        List<User> admins = userRepository.findByRole_RoleName("admin");
        int count = 0;
        for (User admin : admins) {
            notificationService.createAndPush(
                    admin,
                    req.title(),
                    req.message(),
                    req.type() != null ? req.type() : NotificationType.SYSTEM,
                    req.redirectUrl()
            );
            count++;
        }
        return count; // trả số admin đã được đẩy notify
    }

    /** Payload request */
    public record NotificationReq(
            String title,
            String message,
            NotificationType type,   // SYSTEM | MESSAGE | BOOKING | PAYMENT
            String redirectUrl
    ) { }
}

