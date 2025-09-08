package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.UserStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.User}
 */
@Value
public class UserDto implements Serializable {
    String userId;
    String fullName;
    String phoneNumber;
    String email;
    String avatarUrl;
    LocalDateTime lastLogin;
    UserStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}