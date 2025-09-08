package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.entities.enums.PropertyModerationAction;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "property_moderations",
        indexes = {
                @Index(name = "idx_pm_property", columnList = "property_id"),
                @Index(name = "idx_pm_actor", columnList = "actor_id"),
                @Index(name = "idx_pm_created", columnList = "createdAt")
        })
public class PropertyModeration {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    void pre() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropertyModerationAction action; // APPROVE | REJECT

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20, nullable = false)
    private PostStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20, nullable = false)
    private PostStatus toStatus;

    @Column(columnDefinition = "TEXT")
    private String reason; // lý do từ chối (nếu REJECT)

    // admin thực hiện
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    private LocalDateTime createdAt;
}
