package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.UserManagementLog;

public interface UserManagementLogRepository extends JpaRepository<UserManagementLog, String> {
}
