package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="Amenities")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Amenity {
    //Tien nghi
    @Id
    @Column(name="amenity_id", columnDefinition="CHAR(36)") String amenityId;

    @PrePersist
    private void prePersist() {
        if (this.amenityId == null) {
            this.amenityId = java.util.UUID.randomUUID().toString();
        }
    }
    private String amenityName;

    @OneToMany(mappedBy = "amenity", fetch = FetchType.LAZY)
    private List<PropertyAmenity> properties = new ArrayList<>();
}
