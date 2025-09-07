package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "property_furnishings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"property_id","furnishing_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PropertyFurnishing {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    void pre() { if (id == null) id = java.util.UUID.randomUUID().toString(); }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "furnishing_id", nullable = false)
    private Furnishings furnishing;

    @Column(name = "quantity")
    private Integer quantity; // ví dụ: 1 điều hoà, 2 tủ…
}
