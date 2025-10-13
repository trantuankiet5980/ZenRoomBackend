package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.entities.enums.MediaType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_attachments", indexes = {
        @Index(name = "idx_msg_attachment_message", columnList = "message_id"),
        @Index(name = "idx_msg_attachment_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageAttachment {
    @Id
    @Column(name = "attachment_id", columnDefinition = "CHAR(36)")
    private String attachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    @Column(name = "storage_key", length = 512)
    private String storageKey;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long size;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (attachmentId == null) {
            attachmentId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
