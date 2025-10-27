package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.UserManagementLog;

import java.time.LocalDateTime;
import java.util.List;

public interface UserManagementLogRepository extends JpaRepository<UserManagementLog, String> {
    Page<UserManagementLog> findAllByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
    Page<UserManagementLog> findAllByCreatedAtGreaterThanEqual(LocalDateTime from, Pageable pageable);
    Page<UserManagementLog> findAllByCreatedAtLessThanEqual(LocalDateTime to, Pageable pageable);
}
