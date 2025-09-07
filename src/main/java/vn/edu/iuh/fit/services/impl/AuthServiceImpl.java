package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.auths.UserPrincipal;
import vn.edu.iuh.fit.dtos.requests.LoginRequest;
import vn.edu.iuh.fit.dtos.requests.SignUpRequest;
import vn.edu.iuh.fit.dtos.responses.LoginResponse;
import vn.edu.iuh.fit.dtos.responses.RefreshTokenResponse;
import vn.edu.iuh.fit.entities.RefreshToken;
import vn.edu.iuh.fit.entities.Role;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.UserStatus;
import vn.edu.iuh.fit.exceptions.InvalidTokenException;
import vn.edu.iuh.fit.exceptions.MissingTokenException;
import vn.edu.iuh.fit.exceptions.TokenRevokedException;
import vn.edu.iuh.fit.exceptions.UserNotFoundException;
import vn.edu.iuh.fit.repositories.RoleRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.AuthService;
import vn.edu.iuh.fit.services.RefreshTokenService;
import vn.edu.iuh.fit.utils.FormatPhoneNumber;
import vn.edu.iuh.fit.utils.JwtUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtTokenUtil;
    private String jwtEncoder;
    @Autowired
    private JwtDecoder jwtDecoder;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    public boolean signUp(SignUpRequest signUpRequest) {
        String formattedPhone = FormatPhoneNumber.formatPhoneNumberTo0(signUpRequest.getPhoneNumber());
        logger.debug("Checking registration for phone number: {}", formattedPhone);

        // Check if phone number already exists in "0" format
        if (userRepository.existsByPhoneNumber(formattedPhone)) {
            logger.warn("Phone number {} is already registered.", formattedPhone);
            return false;
        }

        // Optional: Check "+84" format for legacy data or input variations
        String formattedPhone84 = FormatPhoneNumber.formatPhoneNumberTo84(signUpRequest.getPhoneNumber());
        if (userRepository.existsByPhoneNumber(formattedPhone84)) {
            logger.warn("Phone number {} (+84 format) is already registered.", formattedPhone84);
            return false;
        }

        String rawPassword = signUpRequest.getPassword();
        if (!rawPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$")) {
            throw new IllegalArgumentException("Password phải có ít nhất 8 ký tự, bao gồm chữ cái và số.");
        }

        List<String> roles = signUpRequest.getRoles();
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required.");
        }

        boolean validRoles = roles.stream().allMatch(role -> role.equals("landlord") || role.equals("tenant"));
        if (!validRoles) {
            throw new IllegalArgumentException("Roles phải là 'landlord' hoặc 'tenant'.");
        }

        // Chỉ định vai trò đầu tiên trong danh sách
        String roleName = roles.get(0);
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        String encodedPassword = passwordEncoder.encode(rawPassword);
        logger.debug("Encoded password for phone {}: {}", formattedPhone, encodedPassword);

        User newUser = User.builder()
                .fullName(signUpRequest.getFullName())
                .phoneNumber(formattedPhone)
                .passwordHash(encodedPassword)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(newUser);
        logger.info("User registered successfully with phone number: {}", formattedPhone);
        return true;
    }

//    @Override
//    public LoginResponse login(LoginRequest loginRequest) {
//        String formattedPhone = FormatPhoneNumber.formatPhoneNumberTo84(loginRequest.getPhoneNumber());
//        User user = userRepository.findByPhoneNumber(formattedPhone)
//                .orElseThrow(() -> new UserNotFoundException("Số điện thoại không tồn tại."));
//
//        if (user.getStatus() == UserStatus.BANNED) {
//            throw new UserNotFoundException("Tài khoản đã bị khóa.");
//        }
//        if (user.getStatus() == UserStatus.INACTIVE) {
//            throw new UserNotFoundException("Tài khoản chưa kích hoạt.");
//        }
//
//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(formattedPhone, loginRequest.getPassword())
//        );
//
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
//        String accessToken = jwtTokenUtil.generateToken(user.getUserId(), user.getRole().getRoleName());
//        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getUserId());
//        String userId = userPrincipal.getUserResponse().getUserId();
//
//        RefreshToken token = RefreshToken.builder()
//                .refreshToken(refreshToken)
//                .id(Long.valueOf(userId))
//                .expiresDate(jwtTokenUtil.generateExpirationDate())
//                .revoked(false)
//                .build();
//
//        refreshTokenService.saveRefreshToken(token);
//
//        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "USER";
//        return LoginResponse.ok(
//                accessToken,
//                roleName,
//                userId,
//                jwtTokenUtil.accessTokenExpiration,
//                "Đăng nhập thành công",
//                refreshToken
//        );
//    }

   @Override
    public void logout(String accessToken) {
        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            throw new MissingTokenException("Token không hợp lệ hoặc không tồn tại.");
        }

        String jwtToken = accessToken.substring(7);
        try {
            Jwt decodedToken = jwtDecoder.decode(jwtToken);
            String userId = decodedToken.getSubject();

            if (userId == null || userId.isEmpty()) {
                throw new IllegalArgumentException("Token không chứa thông tin hợp lệ.");
            }

            boolean isAccessToken = decodedToken.getClaims().containsKey("roles");
            if (!isAccessToken) {
                throw new InvalidTokenException("Bạn đã truyền nhầm Refresh Token thay vì Access Token.");
            }

            UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(userId);
            String userIdFromPrincipal = userPrincipal.getUserResponse().getUserId();
            if (userIdFromPrincipal == null) {
                throw new TokenRevokedException("ID người dùng không hợp lệ.");
            }

            String refreshToken = refreshTokenService.getRefreshTokenByUser(userIdFromPrincipal);
            if (refreshToken == null) {
                throw new IllegalArgumentException("Không tìm thấy refresh token cho người dùng này.");
            }

            RefreshToken storedRefreshToken = refreshTokenService.findByToken(refreshToken);
            storedRefreshToken.setRevoked(true);
            refreshTokenService.saveRefreshToken(storedRefreshToken);
            SecurityContextHolder.clearContext();
        } catch (JwtException e) {
            throw new InvalidTokenException("Token không hợp lệ.");
        }
    }

    @Override
    public RefreshTokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new MissingTokenException("Refresh token không được để trống.");
        }

        try {
            Jwt decodedToken = jwtDecoder.decode(refreshToken);
            String userId = decodedToken.getSubject();

            RefreshToken storedRefreshToken = refreshTokenService.findByToken(refreshToken);
            if (storedRefreshToken == null || storedRefreshToken.isRevoked()) {
                throw new TokenRevokedException("Refresh token đã bị thu hồi. Vui lòng đăng nhập lại.");
            }

            List<RefreshToken> validRefreshTokens = refreshTokenService.getValidTokensByUserId(String.valueOf(storedRefreshToken.getId()));
            if (validRefreshTokens.isEmpty()) {
                throw new TokenRevokedException("Không còn refresh token hợp lệ. Vui lòng đăng nhập lại.");
            }

            UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(userId);
            if (userPrincipal == null) {
                throw new UserNotFoundException("Không tìm thấy người dùng từ token.");
            }

            String newAccessToken = jwtTokenUtil.generateToken(
                    String.valueOf(new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities())),
                    jwtEncoder
            );

            return new RefreshTokenResponse(newAccessToken, "Bearer", jwtTokenUtil.getExpiration());
        } catch (JwtException e) {
            throw new InvalidTokenException("Refresh token không hợp lệ.");
        }
    }
}