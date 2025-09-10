package vn.edu.iuh.fit.dtos.user;

public record UserUpdateRequest(
        String fullName,
        String phoneNumber,
        String email,
        String newPassword,
        String roleId,
        String status,   // ACTIVE|INACTIVE|BANNED
        String avatarUrl,
        String gender,
        String dateOfBirth, // yyyy-MM-dd
        String bio
) {}
