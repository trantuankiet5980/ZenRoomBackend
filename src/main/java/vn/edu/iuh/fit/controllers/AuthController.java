package vn.edu.iuh.fit.controllers;

import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.requests.LoginRequest;
import vn.edu.iuh.fit.dtos.responses.LoginResponse;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.UserStatus;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.utils.JwtUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // 1) Tìm theo phone
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.fail("Số điện thoại không đúng"));
        }

        // 2) Kiểm tra trạng thái
        if (user.getStatus() == UserStatus.BANNED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(LoginResponse.fail("Tài khoản đã bị khóa"));
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(LoginResponse.fail("Tài khoản chưa kích hoạt"));
        }
        // 3) So khớp mật khẩu
        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.fail("Mật khẩu không đúng"));
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        // 4) Tạo token
        String roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "tenant";
        String token = jwtUtil.generateToken(user.getUserId(), roleName);
        Date expiry = new Date(System.currentTimeMillis() + jwtUtil.getExpiration());

        return ResponseEntity.ok(LoginResponse.ok(
                token, roleName, user.getUserId(), user.getFullName(), expiry.getTime(),
                "Đăng nhập thành công"
        ));
    }
}
