package vn.edu.iuh.fit.dtos.user;

public record UserCreateRequest(
        String fullName,
        String phoneNumber,
        String email,
        String password,  // raw; server sẽ hash
        String roleId,
        String avatarUrl
) {}
