package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.UserDto;
import vn.edu.iuh.fit.dtos.requests.SignUpRequest;
import vn.edu.iuh.fit.dtos.user.UserCreateRequest;
import vn.edu.iuh.fit.dtos.user.UserResponse;
import vn.edu.iuh.fit.dtos.user.UserUpdateRequest;
import vn.edu.iuh.fit.entities.Role;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.UserStatus;
import vn.edu.iuh.fit.mappers.UserMapper;
import vn.edu.iuh.fit.repositories.RoleRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.AuthService;
import vn.edu.iuh.fit.services.UserService;
import vn.edu.iuh.fit.utils.FormatPhoneNumber;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final AuthService authService;
    private final UserMapper userMapper;

    @Override
    public UserDto create(UserCreateRequest req) {
        if (req == null) throw new IllegalArgumentException("Request is null");
        if (isBlank(req.fullName()))    throw new IllegalArgumentException("fullName is required");
        if (isBlank(req.phoneNumber())) throw new IllegalArgumentException("phoneNumber is required");
        if (isBlank(req.email()))       throw new IllegalArgumentException("email is required");
        if (isBlank(req.password()))    throw new IllegalArgumentException("password is required");
        if (isBlank(req.roleId()))      throw new IllegalArgumentException("roleId is required");

        if (userRepository.existsByEmail(req.email()))
            throw new IllegalArgumentException("Email already exists");
        if (userRepository.existsByPhoneNumber(req.phoneNumber()))
            throw new IllegalArgumentException("Phone already exists");

        Role role = roleRepository.findById(req.roleId())
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        User u = User.builder()
                .userId(UUID.randomUUID().toString())
                .fullName(req.fullName())
                .phoneNumber(req.phoneNumber())
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .role(role)
                .avatarUrl(req.avatarUrl())
                .status(UserStatus.ACTIVE)
                .gender(req.gender())
                .dateOfBirth(req.dateOfBirth())
                .bio(req.bio())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(u);

        return userMapper.toDto(u);
    }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    @Override
    public Page<UserResponse> list(Pageable pageable, String keyword, LocalDateTime createdFrom, LocalDateTime createdTo) {
        Specification<User> spec = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> {
                var fullName = cb.like(cb.lower(root.get("fullName")), like);
                var phone = cb.like(cb.lower(root.get("phoneNumber")), like);
                var email = cb.like(cb.lower(root.get("email")), like);
                return cb.or(fullName, phone, email);
            });
        }

        if (createdFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }

        if (createdTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }

        return userRepository.findAll(spec, pageable).map(u -> UserResponse.builder()
                .userId(u.getUserId())
                .fullName(u.getFullName())
                .phoneNumber(u.getPhoneNumber())
                .email(u.getEmail())
                .roleId(u.getRole() != null ? u.getRole().getRoleId() : null)
                .roleName(u.getRole() != null ? u.getRole().getRoleName() : null)
                .status(u.getStatus() != null ? u.getStatus().name() : null)
                .avatarUrl(u.getAvatarUrl())
                .createdAt(u.getCreatedAt())
                .deleteRequestedAt(u.getDeleteRequestedAt())
                .deleteEffectiveAt(u.getDeleteEffectiveAt())
                .lastLogin(u.getLastLogin())
                .banReason(u.getBanReason())
                .build());
    }


    @Transactional
    @Override
    public UserDto updateMe(UserDto dto) {
        User me = authService.getCurrentUser();
        applyPatchFromDto(me, dto, /*allowRoleStatus*/ false);
        userRepository.save(me);
        return userMapper.toDto(me);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto getById(String id) {
        User u = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return userMapper.toDto(u);
    }

    @Transactional
    @Override
    public UserDto update(String id, UserDto dto) {
        User u = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        applyPatchFromDto(u, dto, /*allowRoleStatus*/ true);
        userRepository.save(u);
        return userMapper.toDto(u);
    }


    @Override
    public void requestDeleteAccount(String userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        u.setStatus(UserStatus.PENDING_DELETE);
        u.setUpdatedAt(LocalDateTime.now());
        userRepository.save(u);
    }

    /* -------- helper: patch các field từ DTO vào entity -------- */
    private void applyPatchFromDto(User u, UserDto d, boolean allowRoleStatus) {
        if (d.getFullName()!=null && !d.getFullName().isBlank()) u.setFullName(d.getFullName().trim());

        if (d.getPhoneNumber()!=null && !d.getPhoneNumber().isBlank()) {
            String phone0 = FormatPhoneNumber.formatPhoneNumberTo0(d.getPhoneNumber());
            if (userRepository.existsByPhoneNumberAndUserIdNot(phone0, u.getUserId()))
                throw new IllegalArgumentException("Phone number already in use");
            u.setPhoneNumber(phone0);
        }

        if (d.getEmail()!=null && !d.getEmail().isBlank()) {
            String email = d.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmailAndUserIdNot(email, u.getUserId()))
                throw new IllegalArgumentException("Email already in use");
            u.setEmail(email);
        }

        if (d.getAvatarUrl()!=null) u.setAvatarUrl(d.getAvatarUrl());
        if (d.getBio()!=null) u.setBio(d.getBio().trim());
        if (d.getGender()!=null) u.setGender(d.getGender());
        if (d.getDateOfBirth()!=null) u.setDateOfBirth(d.getDateOfBirth());


        if (allowRoleStatus && d.getStatus()!=null) {
            u.setStatus(d.getStatus());
        }

        u.setUpdatedAt(LocalDateTime.now());
    }
}
