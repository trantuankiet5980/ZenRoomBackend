package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "property_amenities",
        uniqueConstraints = @UniqueConstraint(columnNames = {"property_id","amenity_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PropertyAmenity {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    void pre() { if (id == null) id = java.util.UUID.randomUUID().toString(); }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amenity_id", nullable = false)
    private Amenity amenity;
}
