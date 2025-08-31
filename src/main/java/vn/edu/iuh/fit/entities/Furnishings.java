package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="Furnishings")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Furnishings {
    //Noi that
    @Id
    @Column(name="furnishing_id", columnDefinition="CHAR(36)") String furnishingId;
    @PrePersist
    private void prePersist() {
        if (this.furnishingId == null) {
            this.furnishingId = java.util.UUID.randomUUID().toString();
        }
    }
    private String furnishingName;
}
