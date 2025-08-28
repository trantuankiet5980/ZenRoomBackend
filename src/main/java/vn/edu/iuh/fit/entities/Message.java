package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Messages")
public class Message {
    @Id
    @Column(name = "message_id", columnDefinition = "CHAR(36)")
    private String messageId;

    @PrePersist
    void prePersist() {
        if (this.messageId == null) this.messageId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", referencedColumnName = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", referencedColumnName = "user_id")
    private User sender;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at") private LocalDateTime createdAt;

    @Column(name = "is_read")
    private Boolean isRead;
}
