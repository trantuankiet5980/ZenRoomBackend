package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.UserDto;
import vn.edu.iuh.fit.entities.User;

@Component
public class UserMapper {

    /** Entity -> DTO */
    public UserDto toDto(User e) {
        if (e == null) return null;
        return new UserDto(
                e.getUserId(),
                e.getFullName(),
                e.getPhoneNumber(),
                e.getEmail(),
                e.getAvatarUrl(),
                e.getLastLogin(),
                e.getStatus(),
                e.getBanReason(),
                e.getDeleteRequestedAt(),
                e.getDeleteEffectiveAt(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    /** DTO -> Entity (KHÔNG map các quan hệ phức tạp, chỉ set field cơ bản) */
    public User toEntity(UserDto d) {
        if (d == null) return null;
        return User.builder()
                .userId(d.getUserId())
                .fullName(d.getFullName())
                .phoneNumber(d.getPhoneNumber())
                .email(d.getEmail())
                .avatarUrl(d.getAvatarUrl())
                .lastLogin(d.getLastLogin())
                .status(d.getStatus())
                .banReason(d.getBanReason())
                .deleteRequestedAt(d.getDeleteRequestedAt())
                .deleteEffectiveAt(d.getDeleteEffectiveAt())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
