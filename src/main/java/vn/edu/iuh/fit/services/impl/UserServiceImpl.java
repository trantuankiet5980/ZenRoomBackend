package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
    public User create(User user) {
        user.setUserId(UUID.randomUUID().toString());
        user.setPasswordHash(encoder.encode(user.getPasswordHash()));
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        // ---- GÁN ROLE ----
        if (user.getRole() != null) {
            Role incoming = user.getRole();

            if (incoming.getRoleId() != null) {
                Role role = roleRepository.findById(incoming.getRoleId())
                        .orElseThrow(() -> new EntityNotFoundException("Role not found by id: " + incoming.getRoleId()));
                user.setRole(role);
            } else if (incoming.getRoleName() != null) {
                String rn = incoming.getRoleName().toLowerCase(Locale.ROOT).trim();
                Role role = roleRepository.findByRoleName(rn)
                        .orElseThrow(() -> new EntityNotFoundException("Role not found by name: " + rn));
                user.setRole(role);
            } else {
                throw new IllegalArgumentException("Role must contain either roleId or roleName");
            }
        } else {
            throw new IllegalArgumentException("Role is required");
        }
        return userRepository.save(user);
    }

    @Override
    public User getById(String id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Override
    public User update(String id, User user) {
        User existing = getById(id);
        if (user.getFullName() != null) existing.setFullName(user.getFullName());
        if (user.getPhoneNumber() != null) existing.setPhoneNumber(user.getPhoneNumber());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getPasswordHash() != null) existing.setPasswordHash(encoder.encode(user.getPasswordHash()));
        if (user.getAvatarUrl() != null) existing.setAvatarUrl(user.getAvatarUrl());
        if (user.getStatus() != null) existing.setStatus(user.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        userRepository.deleteById(id);
    }
}
