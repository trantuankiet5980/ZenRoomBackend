package vn.edu.iuh.fit.controllers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.requests.SignUpRequest;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;
import vn.edu.iuh.fit.dtos.responses.LoginResponse;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.exceptions.UserNotFoundException;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.AuthService;
import vn.edu.iuh.fit.services.DefaultConversationService;
import vn.edu.iuh.fit.services.SmsService;
import vn.edu.iuh.fit.services.impl.SmsServiceImpl;
import vn.edu.iuh.fit.utils.FormatPhoneNumber;
import vn.edu.iuh.fit.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private AuthService authService;
    @Autowired private SmsService smsService;
    @Autowired private DefaultConversationService defaultConversationService;
    @Autowired private Validator validator;

    // Lưu tạm yêu cầu đăng ký: key = phone0 (0...), value = SignUpRequest
    private final Map<String, SignUpRequest> pendingRegistrations = new HashMap<>();

    // ==============================
    // 1. API: /register → GỬI OTP
    // ==============================
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody @Valid SignUpRequest request) {
        String phone = request.getPhoneNumber();
        String phone84 = FormatPhoneNumber.formatPhoneNumberTo84(phone);
        String phone0 = FormatPhoneNumber.formatPhoneNumberTo0(phone);

        // Kiểm tra trùng
        if (userRepository.existsByPhoneNumber(phone84) || userRepository.existsByPhoneNumber(phone0)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Số điện thoại đã được đăng ký"));
        }
        // Lưu tạm
        pendingRegistrations.put(phone0, request);

        try {
            smsService.sendOtp("+" + phone84); // Gửi thật qua AWS SNS
            logger.info("OTP sent to: +{}", phone84);
            return ResponseEntity.ok(ApiResponse.success("Mã OTP đã được gửi đến: " + phone));
        } catch (Exception e) {
            pendingRegistrations.remove(phone0);
            logger.error("Gửi OTP thất bại: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("Gửi OTP thất bại: " + e.getMessage()));
        }
    }

    // ==============================
    // 2. API: /verify-otp-sns → XÁC THỰC OTP → HOÀN TẤT ĐĂNG KÝ
    // ==============================
    @PostMapping("/verify-otp-sns")
    public ResponseEntity<ApiResponse<?>> verifyOtpSns(@RequestBody Map<String, String> body) {
        String phoneInput = body.get("phoneNumber");
        String otp = body.get("otp");

        if (phoneInput == null || otp == null || phoneInput.isBlank() || otp.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Số điện thoại và OTP là bắt buộc"));
        }

        String phone0 = FormatPhoneNumber.formatPhoneNumberTo0(phoneInput);
        String phone84 = FormatPhoneNumber.formatPhoneNumberTo84(phoneInput);
        String key = "+" + phone84;

        // Xác thực OTP
        if (!smsService.verifyOtp(key, otp)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("OTP không đúng hoặc đã hết hạn"));
        }

        // Lấy yêu cầu tạm
        SignUpRequest pending = pendingRegistrations.remove(phone0);
        if (pending == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy yêu cầu đăng ký. Vui lòng gửi lại."));
        }

        // Đăng ký
        boolean success = authService.signUp(pending);
        if (!success) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Đăng ký thất bại"));
        }

        // Tạo chat với admin
        User user = userRepository.findByPhoneNumber(phone0).orElseThrow();
        defaultConversationService.createConversationWithAdmin(user);

        // Tạo token
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole().getRoleName());

        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công!", Map.of(
                "token", token,
                "userId", user.getUserId(),
                "role", user.getRole().getRoleName()
        )));
    }

    // ==============================
    // CÁC API CŨ (GIỮ NGUYÊN)
    // ==============================


    // Dùng cho quên mật khẩu hoặc kiểm tra số điện thoại đã đăng ký
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<?>> sendOtp(@RequestBody Map<String, String> req) {
        String phone = req.get("phoneNumber");
        if (phone == null || !phone.matches("^(\\+84|0)[3|5|7|8|9]\\d{8}$")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Số điện thoại không hợp lệ"));
        }

        String phone84 = FormatPhoneNumber.formatPhoneNumberTo84(phone);
        String phone0 = "0" + phone84.substring(2);

        // SỬA: Phải là số ĐÃ đăng ký (quên mật khẩu)
        if (!userRepository.existsByPhoneNumber(phone84) && !userRepository.existsByPhoneNumber(phone0)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Số điện thoại chưa được đăng ký"));
        }

        try {
            smsService.sendOtp("+" + phone84);
            return ResponseEntity.ok(ApiResponse.success("OTP đã được gửi"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Gửi OTP thất bại: " + e.getMessage()));
        }
    }

//    @PostMapping(value = "/sign-up", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<ApiResponse<?>> signUp(
//            @RequestPart("request") String json,
//            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
//
//        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
//        SignUpRequest req;
//        try {
//            req = mapper.readValue(json, SignUpRequest.class);
//
//            if (avatar != null && !avatar.isEmpty()) {
//                return ResponseEntity.badRequest().body(ApiResponse.error("Tính năng upload avatar tạm thời bị tắt. Vui lòng gửi avatarUrl trong JSON."));
//            }
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error("JSON không hợp lệ: " + e.getMessage()));
//        }
//
//        Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(req);
//        if (!violations.isEmpty()) {
//            Map<String, String> errors = new HashMap<>();
//            violations.forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
//            return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu không hợp lệ", errors));
//        }
//
//        String phone84 = FormatPhoneNumber.formatPhoneNumberTo84(req.getPhoneNumber());
//        String key = "+" + phone84;
//
//        if (!smsService.verifyOtp(key, req.getOtp())) {
//            return ResponseEntity.badRequest().body(ApiResponse.error("OTP không đúng hoặc đã hết hạn"));
//        }
//
//        String phone0 = "0" + phone84.substring(2);
//        if (userRepository.existsByPhoneNumber(phone84) || userRepository.existsByPhoneNumber(phone0)) {
//            return ResponseEntity.badRequest().body(ApiResponse.error("Số điện thoại đã tồn tại"));
//        }
//
//        boolean success = authService.signUp(req);
//        if (!success) {
//            return ResponseEntity.badRequest().body(ApiResponse.error("Đăng ký thất bại"));
//        }
//
//        User user = userRepository.findByPhoneNumber(phone0).orElseThrow();
//        String token = jwtUtil.generateToken(user.getUserId(), user.getRole().getRoleName());
//        defaultConversationService.createConversationWithAdmin(user);
//
//        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công!", Map.of(
//                "token", token,
//                "userId", user.getUserId(),
//                "role", user.getRole().getRoleName()
//        )));
//    }

    @PostMapping("/sign-in")
    public ResponseEntity<LoginResponse> signIn(@Valid @RequestBody LoginRequest req) {
        String phone0 = FormatPhoneNumber.formatPhoneNumberTo0(req.getPhoneNumber());
        User user = userRepository.findByPhoneNumber(phone0)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() == vn.edu.iuh.fit.entities.enums.UserStatus.BANNED)
            return ResponseEntity.status(403).body(LoginResponse.fail("Tài khoản bị khóa"));
        if (user.getStatus() == vn.edu.iuh.fit.entities.enums.UserStatus.INACTIVE)
            return ResponseEntity.status(403).body(LoginResponse.fail("Tài khoản chưa kích hoạt"));

        if (!encoder.matches(req.getPassword(), user.getPasswordHash()))
            return ResponseEntity.status(401).body(LoginResponse.fail("Mật khẩu sai"));

        String role = user.getRole() != null ? user.getRole().getRoleName() : "tenant";
        String token = jwtUtil.generateToken(user.getUserId(), role);
        long expiry = System.currentTimeMillis() + jwtUtil.getExpiration();

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(LoginResponse.ok(token, role, user.getUserId(), user.getFullName(), expiry));
    }

    @Data
    public static class LoginRequest {
        private String phoneNumber;
        private String password;
    }

    @PostMapping("/verify-otp-firebase")
    public ResponseEntity<ApiResponse<?>> verifyFirebase(@RequestBody Map<String, String> req) {
        String idToken = req.get("idToken");
        try {
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String phone = token.getClaims().get("phone_number").toString();
            if (userRepository.existsByPhoneNumber(phone)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Số điện thoại đã được dùng"));
            }
            return ResponseEntity.ok(ApiResponse.success("Xác thực Firebase thành công", Map.of("phone", phone)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("OTP Firebase thất bại: " + e.getMessage()));
        }
    }


    // đặt lại mật khẩu khi quên
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody Map<String, String> request) {
        String phone = request.get("phoneNumber");
        String newPassword = request.get("newPassword");

        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Phone number cannot be empty.")
                    .data(null)
                    .build());
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("New password cannot be empty.")
                    .data(null)
                    .build());
        }

        logger.debug("Reset password request for phone: {}, new password: {}", phone, newPassword);
        String formattedPhone = FormatPhoneNumber.formatPhoneNumberTo0(phone);

        if (!(smsService instanceof SmsServiceImpl smsServiceImpl) ||
                !smsServiceImpl.isOtpVerified(formattedPhone)) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("You must verify OTP before resetting password.")
                    .data(null)
                    .build());
        }

        if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$")) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Password must be at least 8 characters long, including letters and numbers.")
                    .data(null)
                    .build());
        }

        try {
            authService.resetPassword(formattedPhone, newPassword);
            smsServiceImpl.clearOtpVerification(formattedPhone);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Password reset successfully.")
                    .data(null)
                    .build());
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }

    // đổi mật khẩu khi đã đăng nhập
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<?>> changePassword(@RequestBody Map<String, String> request) {
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        if (currentPassword == null || currentPassword.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Current password and new password cannot be empty")
                    .data(null)
                    .build());
        }

        try {
            authService.changePassword(currentPassword, newPassword);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Password changed successfully")
                    .data(null)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.builder()
                    .success(false)
                    .message("Failed to change password: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<ApiResponse<?>> verifyResetOtp(@RequestBody Map<String, String> request) {
        String phone = request.get("phoneNumber");
        String otp = request.get("otp");

        if (phone == null || otp == null || phone.trim().isEmpty() || otp.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Phone number or OTP cannot be empty"));
        }

        // SỬA: Dùng +84 để verify
        String phoneForVerify = "+" + FormatPhoneNumber.formatPhoneNumberTo84(phone);
        boolean isValid = smsService.verifyOtp(phoneForVerify, otp);

        if (!isValid) {
            return ResponseEntity.badRequest().body(ApiResponse.error("OTP không đúng hoặc đã hết hạn"));
        }

        String formattedPhone = FormatPhoneNumber.formatPhoneNumberTo0(phone);
        if (smsService instanceof SmsServiceImpl smsServiceImpl) {
            smsServiceImpl.setOtpVerified(formattedPhone, true);
        }

        return ResponseEntity.ok(ApiResponse.success("OTP hợp lệ"));
    }

}