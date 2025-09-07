package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.entities.enums.PostStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "Posts",
        indexes = {
                @Index(name = "idx_listings_status", columnList = "status"),
                @Index(name = "idx_listings_landlord", columnList = "landlord_id"),
                @Index(name = "idx_listings_property", columnList = "property_id"),
                @Index(name = "idx_listings_published_at", columnList = "published_at")
        }
)
public class Post {
    @Id
    @Column(name = "post_id", columnDefinition = "CHAR(36)")
    private String postId;

    @PrePersist
    void prePersist() {
        if (postId == null) postId = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = PostStatus.PENDING;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Chủ nhà đăng bài
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    // Chọn phòng/tòa đã lưu (property)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Tick đảm bảo PCCC
    @Column(name = "is_fire_safe", nullable = false)
    private Boolean isFireSafe = false;

    // SĐT liên hệ hiển thị ở bài đăng
    @Column(name = "contact_phone", length = 20, nullable = false)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PostStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt; // set khi duyệt/đăng

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
