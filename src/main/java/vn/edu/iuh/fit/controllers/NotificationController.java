package vn.edu.iuh.fit.controllers;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.NotificationDto;
import vn.edu.iuh.fit.entities.Notification;
import vn.edu.iuh.fit.mappers.NotificationMapper;
import vn.edu.iuh.fit.repositories.NotificationRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @GetMapping
    public List<NotificationDto> list(Authentication auth) {
        String userId = auth.getName();
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }
    @PostMapping("/{id}/read")
    public void markRead(@PathVariable String id, Authentication auth) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        // kiểm tra quyền theo userId
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    @PostMapping("/read-all")
    public void markReadAll(Authentication auth) {
        String userId = auth.getName();
        List<Notification> notifications = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }
}
