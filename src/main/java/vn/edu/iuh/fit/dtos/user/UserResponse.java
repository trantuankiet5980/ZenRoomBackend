package vn.edu.iuh.fit.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class UserResponse {
    private String userId;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String roleId;
    private String roleName;
    private String status;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime deleteRequestedAt;
    private LocalDateTime deleteEffectiveAt;
    private LocalDateTime lastLogin;
    private String banReason;
}