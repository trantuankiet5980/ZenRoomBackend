package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;

import vn.edu.iuh.fit.entities.enums.NotificationType;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Notifications")
public class Notification {
    @Id @Column(name="notification_id", columnDefinition="CHAR(36)") String notificationId;
    @PrePersist
    private void prePersist() {
        if (this.notificationId == null) {
            this.notificationId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = false;
        }
    }
    @ManyToOne @JoinColumn(name="user_id") private User user;
    private String title;
    private String message;
    @Enumerated(EnumType.STRING) private NotificationType type;
    private String redirectUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
