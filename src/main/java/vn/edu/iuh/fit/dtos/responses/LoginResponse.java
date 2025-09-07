package vn.edu.iuh.fit.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
@AllArgsConstructor
public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private String role;
    private String userId;
    private long expiresAt;

    public LoginResponse() {

    }

    public static LoginResponse ok(String token, String role, String userId, long exp) {
        LoginResponse r = new LoginResponse();
        r.success = true; r.message = "Login successful";
        r.token = token; r.role = role; r.userId = userId; r.expiresAt = exp;
        return r;
    }
    public static LoginResponse fail(String msg) {
        LoginResponse r = new LoginResponse();
        r.success = false; r.message = msg;
        return r;
    }

}
