package vn.edu.iuh.fit.controllers;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.responses.LoginResponse;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.UserStatus;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.utils.JwtUtil;

import java.util.Date;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody vn.edu.iuh.fit.dtos.requests.LoginRequest request) {
        String phone = request.getPhoneNumber();
        User user = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getStatus() == UserStatus.BANNED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(LoginResponse.fail("Account is banned"));
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(LoginResponse.fail("Account is inactive"));
        }

        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.fail("Invalid phone number or password"));
        }
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "tenant";
        String token = jwtUtil.generateToken(user.getUserId(), roleName);
        Date expiry = new Date(System.currentTimeMillis() + jwtUtil.getExpiration());

        return ResponseEntity.ok(LoginResponse.ok(token, roleName, user.getUserId(), expiry.getTime()));
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
