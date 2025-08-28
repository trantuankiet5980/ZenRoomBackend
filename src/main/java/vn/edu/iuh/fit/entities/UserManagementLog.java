package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "UserManagementLog")
public class UserManagementLog {
    @Id
    @Column(name = "log_id", columnDefinition = "CHAR(36)")
    private String logId;

    @PrePersist
    void prePersist() {
        if (this.logId == null) this.logId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", referencedColumnName = "user_id")
    private User admin;

    @Column(name = "action", length = 255)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user", referencedColumnName = "user_id")
    private User targetUser;

    @Column(name = "created_at") private LocalDateTime createdAt;
}
