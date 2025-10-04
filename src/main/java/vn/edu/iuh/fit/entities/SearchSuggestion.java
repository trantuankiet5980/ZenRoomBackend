package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "search_suggestions",
        indexes = {
                @Index(name = "idx_suggestion_normalized", columnList = "normalized_text"),
                @Index(name = "idx_suggestion_terms", columnList = "normalized_terms"),
                @Index(name = "idx_suggestion_reference", columnList = "reference_type, reference_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestion {

    @Id
    @Column(name = "suggestion_id", columnDefinition = "CHAR(36)")
    private String suggestionId;

    @Column(name = "reference_type", length = 40, nullable = false)
    private String referenceType;

    @Column(name = "reference_id", length = 64, nullable = false)
    private String referenceId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "subtitle", length = 255)
    private String subtitle;

    @Column(name = "price", precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "normalized_text", length = 512, nullable = false)
    private String normalizedText;

    @Column(name = "normalized_terms", length = 512, nullable = false)
    private String normalizedTerms;

    @Column(name = "popularity_weight")
    private Double popularityWeight;

    @Column(name = "query_count")
    private long queryCount;

    @Column(name = "click_count")
    private long clickCount;

    @Column(name = "last_interacted_at")
    private LocalDateTime lastInteractedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (suggestionId == null) suggestionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (popularityWeight == null) {
            popularityWeight = 1.0d;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
