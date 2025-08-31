package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "UserManagementLog")
public class UserManagementLog {
    @Id @Column(name="log_id", columnDefinition="CHAR(36)") String logId;
    @PrePersist
    private void prePersist() {
        if (this.logId == null) {
            this.logId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
    @ManyToOne @JoinColumn(name="admin_id") private User admin;
    private String action;
    @ManyToOne @JoinColumn(name="target_user") private User targetUser;
    private LocalDateTime createdAt;
}
