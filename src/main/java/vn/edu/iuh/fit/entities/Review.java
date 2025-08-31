package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Reviews")
public class Review {
    @Id @Column(name="review_id", columnDefinition="CHAR(36)") String reviewId;
    @PrePersist
    private void prePersist() {
        if (this.reviewId == null) {
            this.reviewId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
    @ManyToOne @JoinColumn(name="booking_id") private Booking booking;
    @ManyToOne @JoinColumn(name="tenant_id") private User tenant;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
