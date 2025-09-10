package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.token.TokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.UserManagementLog;
import vn.edu.iuh.fit.entities.enums.UserStatus;
import vn.edu.iuh.fit.repositories.UserManagementLogRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.AdminService;
import vn.edu.iuh.fit.services.AuthService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UserManagementLogRepository userManagementLogRepository;
    private final AuthService authService;

    private void logAction(User admin, User target, String action){
        UserManagementLog log = UserManagementLog.builder()
                .admin(admin)
                .targetUser(target)
                .action(action)
                .createdAt(LocalDateTime.now())
                .build();
        userManagementLogRepository.save(log);
    }

    @Transactional
    @Override
    public void processDeletionRequest(String userId, boolean approve, String reason) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (approve) {
            // Chỉ approve nếu đang ở trạng thái PENDING_DELETION
            if (u.getStatus() != UserStatus.PENDING_DELETE){
                throw new IllegalStateException("User is not in PENDING_DELETE status");
            }
            u.setStatus(UserStatus.DELETED);
            u.setDeleteEffectiveAt(LocalDateTime.now().plusDays(30));
        } else {
            if (u.getStatus() != UserStatus.PENDING_DELETE && u.getStatus() != UserStatus.DELETED) {
                throw new IllegalStateException("User is not in PENDING_DELETE or DELETED status");
            }
            // Từ chôi yêu cầu xóa, đưa user về ACTIVE
            if (u.getStatus() == UserStatus.PENDING_DELETE) {
                u.setStatus(UserStatus.ACTIVE);
            }
            u.setDeleteRequestedAt(null);
            u.setDeleteEffectiveAt(null);
        }
        u.setUpdatedAt(LocalDateTime.now());
        userRepository.save(u);

        // Log action
        User admin = authService.getCurrentUser();
        logAction(admin, u, "PROCESS_DELETION: " + (approve ? "APPROVED" : "REJECTED") + (reason != null ? " REASON: " + reason : ""));
    }

    @Override
    public void banUser(String userId, String reason) {

    }

    @Override
    public void unbanUser(String userId) {

    }

    @Override
    public void hardDelete(String userId) {

    }
}
