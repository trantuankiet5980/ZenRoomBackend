package vn.edu.iuh.fit.controllers;

import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.utils.JwtUtil;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String role) {
        // In a real application, you would validate the username and password
        String token = jwtUtil.generateToken(username, role);
        return ResponseEntity.ok(new JwtResponse(token));
    }
    @Data
    public static class LoginRequest {
        private String userId;    // ví dụ: UUID của user
        private String roleName;  // tenant | landlord | admin
    }

    @Data
    public static class JwtResponse {
        private final String token;
    }
}
