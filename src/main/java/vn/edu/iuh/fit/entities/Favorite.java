package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;

@Entity @Table(name="Favorites",
        uniqueConstraints=@UniqueConstraint(columnNames={"tenant_id","property_id"}))
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Favorite {
    @Id @Column(name="favorite_id", columnDefinition="CHAR(36)") String favoriteId;
    @PrePersist
    private void prePersist() {
        if (this.favoriteId == null) {
            this.favoriteId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
    @ManyToOne @JoinColumn(name="tenant_id") private User tenant;
    @ManyToOne @JoinColumn(name="property_id") private Property property;
    private LocalDateTime createdAt;
}
