package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Messages")
public class Message {
    @Id @Column(name="message_id", columnDefinition="CHAR(36)") String messageId;
    @PrePersist
    private void prePersist() {
        if (this.messageId == null) {
            this.messageId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = false;
        }
    }
    @ManyToOne @JoinColumn(name="conversation_id") private Conversation conversation;
    @ManyToOne @JoinColumn(name="sender_id") private User sender;
    private String content;
    private LocalDateTime createdAt;
    private Boolean isRead;
}
