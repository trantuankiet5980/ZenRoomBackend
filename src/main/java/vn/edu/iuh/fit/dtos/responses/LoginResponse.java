package vn.edu.iuh.fit.dtos.responses;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private String role;
    private String userId;
    private long expiresAt;

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

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
    public String getRole() { return role; }
    public String getUserId() { return userId; }
    public long getExpiresAt() { return expiresAt; }
}
