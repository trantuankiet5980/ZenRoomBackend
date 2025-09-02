package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Conversations")
public class Conversation {
    @Id @Column(name="conversation_id", columnDefinition="CHAR(36)") String conversationId;
    @PrePersist
    private void prePersist() {
        if (this.conversationId == null) {
            this.conversationId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
    @ManyToOne @JoinColumn(name="tenant_id") private User tenant;
    @ManyToOne @JoinColumn(name="landlord_id") private User landlord;
    @ManyToOne @JoinColumn(name="property_id") private Property property;
    private LocalDateTime createdAt;
}
