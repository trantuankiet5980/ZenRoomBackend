package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.NotificationType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Notification}
 */
@Value
public class NotificationDto implements Serializable {
    String notificationId;
    UserDto user;
    String title;
    String message;
    NotificationType type;
    String redirectUrl;
    Boolean isRead;
    LocalDateTime createdAt;
}