package vn.edu.iuh.fit.dtos.user;

import vn.edu.iuh.fit.entities.enums.Gender;

import java.time.LocalDateTime;

public record UserCreateRequest(
        String fullName,
        String phoneNumber,
        String email,
        String password,  // raw; server sẽ hash
        String roleId,
        String avatarUrl,
        Gender gender,
        LocalDateTime dateOfBirth,
        String bio
) {}
