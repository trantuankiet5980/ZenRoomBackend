package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "ReviewReplies")
public class ReviewReply {
    @Id
    @Column(name = "reply_id", columnDefinition = "CHAR(36)")
    private String replyId;

    @PrePersist
    void prePersist() {
        if (this.replyId == null) this.replyId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", referencedColumnName = "review_id")
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", referencedColumnName = "user_id")
    private User landlord;

    @Lob
    @Column(name = "reply_text", columnDefinition = "TEXT")
    private String replyText;

    @Column(name = "created_at") private LocalDateTime createdAt;
}
