package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;

import vn.edu.iuh.fit.entity.enums.NotificationType;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Notifications")
public class Notification {
    @Id
    @Column(name = "notification_id", columnDefinition = "CHAR(36)")
    private String notificationId;

    @PrePersist
    void prePersist() {
        if (this.notificationId == null) this.notificationId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User user;

    @Column(name = "title", length = 255) private String title;

    @Lob
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private NotificationType type;

    @Column(name = "redirect_url", length = 255) private String redirectUrl;

    @Column(name = "is_read") private Boolean isRead;

    @Column(name = "created_at") private LocalDateTime createdAt;
}
