package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="Messages",
        indexes = {@Index(name="idx_msg_conversation", columnList="conversation_id"),
                @Index(name="idx_msg_property", columnList="property_id"),
                @Index(name="idx_msg_created", columnList="created_at")}
)
public class Message {
    @Id @Column(name="message_id", columnDefinition="CHAR(36)") String messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="conversation_id", nullable=false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="sender_id", nullable=false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="property_id")
    private Property property;

    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageAttachment> attachments = new ArrayList<>();

    @PrePersist
    void pre() {
        if (messageId == null) messageId = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = java.time.LocalDateTime.now();
        if (isRead == null) isRead = false;
        if (attachments == null) attachments = new ArrayList<>();
    }
}
