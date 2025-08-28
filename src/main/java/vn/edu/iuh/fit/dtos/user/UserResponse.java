package vn.edu.iuh.fit.dtos.user;


public record UserResponse(
        String userId,
        String fullName,
        String phoneNumber,
        String email,
        String roleId,
        String roleName,
        String status,
        String avatarUrl
) {}
