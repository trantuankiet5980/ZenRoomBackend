package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.UserManagementLog;

import java.time.LocalDateTime;
import java.util.List;

public interface UserManagementLogRepository extends JpaRepository<UserManagementLog, String> {
    List<UserManagementLog> findAllByOrderByCreatedAtDesc();
    List<UserManagementLog> findAllByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
    List<UserManagementLog> findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(LocalDateTime from);
    List<UserManagementLog> findAllByCreatedAtLessThanEqualOrderByCreatedAtDesc(LocalDateTime to);
}
