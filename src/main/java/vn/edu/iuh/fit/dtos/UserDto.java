package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.Gender;
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
    String banReason;
    LocalDateTime deleteRequestedAt;  // khi user gửi yêu cầu
    LocalDateTime deleteEffectiveAt;  // thời điểm sẽ xóa cứng (approve + 30 ngày)
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    Gender gender;
    LocalDateTime dateOfBirth;
    String bio;

    // optional: thống kê
    Long followers;  // nullable
    Long following;  // nullable
}