package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import vn.edu.iuh.fit.entities.enums.PostAuditAction;
import vn.edu.iuh.fit.entities.enums.PostStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_audits",
        indexes = {
                @Index(name="idx_post_audits_post", columnList="post_id"),
                @Index(name="idx_post_audits_actor", columnList="actor_id"),
                @Index(name="idx_post_audits_created_at", columnList="created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostAudit {
    @Id
    @Column(name="audit_id", columnDefinition="CHAR(36)")
    private String auditId;

    @PrePersist void pre(){
        if (auditId == null)
            auditId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="post_id", nullable=false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name="action", length=20, nullable=false)
    private PostAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name="from_status", length=20, nullable=false)
    private PostStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name="to_status", length=20, nullable=false)
    private PostStatus toStatus;

    @Column(name="reason", columnDefinition="TEXT")
    private String reason; // null nếu approve

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="actor_id")
    private User actor; // admin thao tác

    @CreationTimestamp
    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;
}
