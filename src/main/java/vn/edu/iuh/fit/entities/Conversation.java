package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "Conversations",
        uniqueConstraints = {
                // Chỉ chống trùng cho CASE CÓ PROPERTY
                @UniqueConstraint(
                        name = "uk_conv_property_tenant_landlord",
                        columnNames = {"property_id","tenant_id","landlord_id"}
                )
        },
        indexes = {
                @Index(name = "idx_conv_property",  columnList = "property_id"),
                @Index(name = "idx_conv_tenant",    columnList = "tenant_id"),
                @Index(name = "idx_conv_landlord",  columnList = "landlord_id"),
                @Index(name = "idx_conv_created_at",columnList = "createdAt")
        }
)
public class Conversation {
    @Id
    @Column(name="conversation_id", columnDefinition="CHAR(36)")
    private String conversationId;

    @PrePersist
    private void prePersist() {
        if (this.conversationId == null) this.conversationId = UUID.randomUUID().toString();
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="tenant_id")
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="landlord_id")
    private User landlord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="property_id")
    private Property property;

    private LocalDateTime createdAt;
}
