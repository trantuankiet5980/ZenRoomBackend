package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.entities.enums.ChargeBasis;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "property_service_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyServiceItem {
    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "service_name", length = 100, nullable = false)
    private String serviceName;

    @Column(name = "fee", precision = 12, scale = 2, nullable = false)
    private BigDecimal fee;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_basis", length = 30, nullable = false)
    private ChargeBasis chargeBasis;

    @Column(name = "is_included")
    private Boolean isIncluded;

    @Column(name = "note", length = 255)
    private String note;
}
