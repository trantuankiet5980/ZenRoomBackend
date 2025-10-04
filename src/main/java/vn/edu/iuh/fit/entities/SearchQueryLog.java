package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "search_query_logs",
        indexes = {
                @Index(name = "idx_query_log_normalized", columnList = "normalized_query"),
                @Index(name = "idx_query_log_suggestion", columnList = "suggestion_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchQueryLog {
    @Id
    @Column(name = "log_id", columnDefinition = "CHAR(36)")
    private String logId;

    @Column(name = "raw_query", length = 255)
    private String rawQuery;

    @Column(name = "normalized_query", length = 255, nullable = false)
    private String normalizedQuery;

    @Column(name = "suggestion_id", columnDefinition = "CHAR(36)")
    private String suggestionId;

    @Column(name = "query_count")
    private long queryCount;

    @Column(name = "click_count")
    private long clickCount;

    @Column(name = "last_occurred_at")
    private LocalDateTime lastOccurredAt;

    @PrePersist
    public void prePersist() {
        if (logId == null) {
            logId = UUID.randomUUID().toString();
        }
        if (lastOccurredAt == null) {
            lastOccurredAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        lastOccurredAt = LocalDateTime.now();
    }

    public void incrementQuery(String latestRawQuery) {
        this.queryCount++;
        this.rawQuery = latestRawQuery;
        this.lastOccurredAt = LocalDateTime.now();
    }

    public void incrementClick(String latestRawQuery) {
        this.clickCount++;
        this.rawQuery = latestRawQuery;
        this.lastOccurredAt = LocalDateTime.now();
    }
}
