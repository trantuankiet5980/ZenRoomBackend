package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "SearchHistory")
public class SearchHistory {
    @Id
    @Column(name = "search_id", columnDefinition = "CHAR(36)")
    private String searchId;

    @PrePersist
    void prePersist() {
        if (this.searchId == null) this.searchId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "user_id")
    private User tenant;

    @Column(name = "keyword", length = 255)
    private String keyword;

    @Lob
    @Column(name = "filters", columnDefinition = "JSON")
    private String filters;

    @Column(name = "created_at") private LocalDateTime createdAt;
}
