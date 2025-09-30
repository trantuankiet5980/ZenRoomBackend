package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "ReviewReplies")
public class ReviewReply {
    @Id @Column(name="reply_id", columnDefinition="CHAR(36)") String replyId;
    @PrePersist
    private void prePersist() {
        if (this.replyId == null) {
            this.replyId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne
    @JoinColumn(name="review_id", unique = true)
    private Review review;
    @ManyToOne @JoinColumn(name="landlord_id") private User landlord;
    private String replyText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
