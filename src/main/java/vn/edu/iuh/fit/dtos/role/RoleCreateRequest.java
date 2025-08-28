package vn.edu.iuh.fit.dtos.role;

public record RoleCreateRequest (
    String roleName // tenant|landlord|admin
) {}

