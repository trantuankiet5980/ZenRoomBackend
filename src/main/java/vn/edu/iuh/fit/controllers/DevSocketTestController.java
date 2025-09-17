package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevSocketTestController {
    private final SimpMessagingTemplate messaging;

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
    @PostMapping("/ping-user/{userId}")
    public Map<String, String> pingUser(@PathVariable String userId) {
        var payload = Map.of(
                "type","PROPERTY_STATUS_CHANGED",
                "title","Bài đăng demo",
                "status","APPROVED",
                "createdAt", java.time.LocalDateTime.now().toString()
        );
        messaging.convertAndSendToUser(userId, "/queue/notifications", payload);
        return payload;
    }
}

