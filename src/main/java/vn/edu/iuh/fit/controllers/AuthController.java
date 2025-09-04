package vn.edu.iuh.fit.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.requests.LoginRequest;
import vn.edu.iuh.fit.dtos.requests.SignUpRequest;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;
import vn.edu.iuh.fit.dtos.responses.LoginResponse;
import vn.edu.iuh.fit.dtos.responses.RefreshTokenResponse;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.UserStatus;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.AuthService;
import vn.edu.iuh.fit.services.SmsService;
import vn.edu.iuh.fit.utils.FormatPhoneNumber;
import vn.edu.iuh.fit.utils.JwtUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private JwtUtil jwtTokenUtil;
    @Autowired
    private AuthService authService;
    @Autowired
    private SmsService smsService;
    @Autowired
    private Validator validator;

    private final Map<String, SignUpRequest> pendingRegistrations = new HashMap<>();

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String phoneNumber0 = FormatPhoneNumber.formatPhoneNumberTo0(request.getPhoneNumber());
        String phoneNumber84 = FormatPhoneNumber.formatPhoneNumberTo84(request.getPhoneNumber());

        User user = userRepository.findByPhoneNumber(phoneNumber0)
                .orElseGet(() -> userRepository.findByPhoneNumber(phoneNumber84).orElse(null));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.fail("Số điện thoại không đúng"));
        }

        if (user.getStatus() == UserStatus.BANNED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(LoginResponse.fail("Tài khoản đã bị khóa"));
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(LoginResponse.fail("Tài khoản chưa kích hoạt"));
        }

        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.fail("Mật khẩu không đúng"));
        }

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "USER";
        String token = jwtTokenUtil.generateToken(user.getUserId(), roleName);
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getUserId());

        return ResponseEntity.ok(LoginResponse.ok(
                token, roleName, user.getUserId(), jwtTokenUtil.getExpiration(), "Đăng nhập thành công", refreshToken
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody String signUpRequestJson) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        SignUpRequest signUpRequest;
        try {
            signUpRequest = objectMapper.readValue(signUpRequestJson, SignUpRequest.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.builder()
                    .success(false)
                    .message("Invalid request format: " + e.getMessage())
                    .data(null)
                    .build());
        }

        Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(signUpRequest);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            violations.forEach(violation -> errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Validation failed")
                    .data(errors)
                    .build());
        }

        String formattedPhone = FormatPhoneNumber.formatPhoneNumberTo0(signUpRequest.getPhoneNumber());
        logger.debug("Checking registration for phone number: {}", formattedPhone);

        // Check if phone number already exists
        if (userRepository.existsByPhoneNumber(formattedPhone)) {
            logger.warn("Phone number {} is already registered.", formattedPhone);
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Phone number already registered!")
                    .data(null)
                    .build());
        }

        // Optional: Check "+84" format for legacy data
        String formattedPhone84 = FormatPhoneNumber.formatPhoneNumberTo84(signUpRequest.getPhoneNumber());
        if (userRepository.existsByPhoneNumber(formattedPhone84)) {
            logger.warn("Phone number {} (+84 format) is already registered.", formattedPhone84);
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Phone number already registered!")
                    .data(null)
                    .build());
        }

        pendingRegistrations.put(formattedPhone, signUpRequest);

        try {
            smsService.sendOtp(formattedPhone);
            logger.info("OTP sent successfully to phone number: {}", formattedPhone);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("OTP sent to phone number: " + formattedPhone)
                    .data(null)
                    .build());
        } catch (Exception e) {
            pendingRegistrations.remove(formattedPhone);
            logger.error("Failed to send OTP to {}: {}", formattedPhone, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Failed to send OTP: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/verify-otp-sns")
    public ResponseEntity<ApiResponse<?>> verifyOtpSns(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        String otp = request.get("otp");

        if (phoneNumber == null || otp == null || phoneNumber.trim().isEmpty() || otp.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Phone number or OTP cannot be empty")
                    .data(null)
                    .build());
        }

        String formattedPhone = FormatPhoneNumber.formatPhoneNumberTo0(phoneNumber);
        boolean isValid = smsService.verifyOtp(formattedPhone, otp);
        if (!isValid) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("OTP verification failed! Please check the OTP or try resending it.")
                    .data(null)
                    .build());
        }

        SignUpRequest signUpRequest = pendingRegistrations.remove(formattedPhone);
        if (signUpRequest == null) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("No pending registration found for phone number: " + formattedPhone)
                    .data(null)
                    .build());
        }

        boolean success = authService.signUp(signUpRequest);
        if (success) {
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("User registered successfully!")
                    .data(null)
                    .build());
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Registration failed!")
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        try {
            RefreshTokenResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(ApiResponse.<RefreshTokenResponse>builder()
                    .success(true)
                    .message("Token refreshed successfully")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<RefreshTokenResponse>builder()
                    .success(false)
                    .message("Failed to refresh token: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(@RequestHeader("Authorization") String accessToken) {
        authService.logout(accessToken);
        return ResponseEntity.ok(ApiResponse.builder()
                .message("SUCCESS")
                .data(null)
                .message("Logout successfully!")
                .build());
    }


}