package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.entities.enums.ChargeBasis;

import java.math.BigDecimal;

@Entity
@Table(name = "property_services",
        uniqueConstraints = @UniqueConstraint(columnNames = {"property_id","service_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PropertyService {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    void pre() { if (id == null) id = java.util.UUID.randomUUID().toString(); }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "fee", precision = 12, scale = 2)
    private BigDecimal fee;           // override phí (nếu null → dùng defaultFee của Service)

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_basis", length = 10)
    private ChargeBasis chargeBasis;  // override basis (nếu null → dùng basis của Service)

    @Column(name = "is_included")
    private Boolean isIncluded;       // đã bao gồm trong giá phòng?

    @Column(name = "note", length = 255)
    private String note;
}
