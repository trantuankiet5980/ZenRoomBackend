package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Reviews")
public class Review {
    @Id
    @Column(name = "review_id", columnDefinition = "CHAR(36)")
    private String reviewId;

    @PrePersist
    void prePersist() {
        if (this.reviewId == null) this.reviewId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", referencedColumnName = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "user_id")
    private User tenant;

    @Column(name = "rating")
    private Integer rating;

    @Lob
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at") private LocalDateTime createdAt;

    @OneToMany(mappedBy = "review", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<ReviewReply> replies = new ArrayList<>();
}
