package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.entities.enums.PropertyModerationAction;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.PropertyModeration}
 */
@Value
public class PropertyModerationDto implements Serializable {
    String id;
    PropertyDto property;
    PropertyModerationAction action;
    PostStatus fromStatus;
    PostStatus toStatus;
    String reason;
    UserDto actor;
    LocalDateTime createdAt;
}