package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Favorites")
public class Favorite {
    @Id
    @Column(name = "favorite_id", columnDefinition = "CHAR(36)")
    private String favoriteId;

    @PrePersist
    void prePersist() {
        if (this.favoriteId == null) this.favoriteId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "user_id")
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", referencedColumnName = "room_id")
    private Room room;

    @Column(name = "created_at") private LocalDateTime createdAt;
}
