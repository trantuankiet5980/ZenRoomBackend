package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.requests.SignUpRequest;
import vn.edu.iuh.fit.dtos.user.UserCreateRequest;
import vn.edu.iuh.fit.dtos.user.UserResponse;
import vn.edu.iuh.fit.dtos.user.UserUpdateRequest;
import vn.edu.iuh.fit.entities.Role;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.UserStatus;
import vn.edu.iuh.fit.repositories.RoleRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
    }
    @Override
    public UserResponse create(UserCreateRequest req) {
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

        User u = new User();
        u.setUserId(UUID.randomUUID().toString());
        u.setFullName(req.fullName());
        u.setPhoneNumber(req.phoneNumber());
        u.setEmail(req.email());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(role);
        u.setAvatarUrl(req.avatarUrl());
        u.setStatus(UserStatus.ACTIVE);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());

        userRepository.save(u);

        return new UserResponse(
                u.getUserId(), u.getFullName(), u.getPhoneNumber(), u.getEmail(),
                role.getRoleId(), role.getRoleName(),
                u.getStatus().name(), u.getAvatarUrl()
        );
    }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    @Override
    public UserResponse getById(String id) {
        User u = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return new UserResponse(
                u.getUserId(), u.getFullName(), u.getPhoneNumber(), u.getEmail(),
                u.getRole()!=null? u.getRole().getRoleId():null,
                u.getRole()!=null? u.getRole().getRoleName():null,
                u.getStatus()!=null? u.getStatus().name():null,
                u.getAvatarUrl()
        );    }

    @Override
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(u -> new UserResponse(
                u.getUserId(), u.getFullName(), u.getPhoneNumber(), u.getEmail(),
                u.getRole()!=null? u.getRole().getRoleId():null,
                u.getRole()!=null? u.getRole().getRoleName():null,
                u.getStatus()!=null? u.getStatus().name():null,
                u.getAvatarUrl()
        ));
    }

    @Override
    public UserResponse update(String id, UserUpdateRequest req) {
        User u = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (req.fullName()!=null && !req.fullName().isBlank())       u.setFullName(req.fullName());
        if (req.phoneNumber()!=null && !req.phoneNumber().isBlank()) u.setPhoneNumber(req.phoneNumber());
        if (req.email()!=null && !req.email().isBlank())             u.setEmail(req.email());
        if (req.avatarUrl()!=null)                                   u.setAvatarUrl(req.avatarUrl());

        if (req.newPassword()!=null && !req.newPassword().isBlank()) {
            u.setPasswordHash(encoder.encode(req.newPassword()));
        }

        if (req.roleId()!=null && !req.roleId().isBlank()) {
            Role role = roleRepository.findById(req.roleId())
                    .orElseThrow(() -> new EntityNotFoundException("Role not found"));
            u.setRole(role);
        }

        if (req.status()!=null && !req.status().isBlank()) {
            String s = req.status().toUpperCase(Locale.ROOT).trim();
            u.setStatus(UserStatus.valueOf(s)); // ACTIVE|INACTIVE|BANNED
        }

        u.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(u.getRole());

        return new UserResponse(
                u.getUserId(), u.getFullName(), u.getPhoneNumber(), u.getEmail(),
                u.getRole()!=null? u.getRole().getRoleId():null,
                u.getRole()!=null? u.getRole().getRoleName():null,
                u.getStatus()!=null? u.getStatus().name():null,
                u.getAvatarUrl()
        );
    }

    @Override
    public void delete(String id) {
        userRepository.deleteById(id);
    }

    @Override
    public void requestDeleteAccount(String userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        u.setStatus(UserStatus.PENDING_DELETE);
        u.setUpdatedAt(LocalDateTime.now());
        userRepository.save(u);
    }
}
