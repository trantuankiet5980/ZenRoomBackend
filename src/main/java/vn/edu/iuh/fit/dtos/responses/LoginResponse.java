package vn.edu.iuh.fit.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private String role;
    private String userId;
    private String fullName;
    private Long  expiresAt;

    public static LoginResponse ok(String token, String role, String userId, String fullName, Long exp, String msg) {
        return new LoginResponse(true, msg, token, role, userId, fullName, exp);
    }
    public static LoginResponse fail(String msg) {
        return new LoginResponse(false, msg, null, null, null, null, null);
    }
}
