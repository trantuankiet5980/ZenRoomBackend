package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.entities.enums.ChargeBasis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="Services")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Service {
    @Id
    @Column(name="service_id", columnDefinition="CHAR(36)") String serviceId;
    @PrePersist
    void pre(){ if(serviceId==null) serviceId= java.util.UUID.randomUUID().toString(); }
    private String serviceName;
    private BigDecimal defaultFee;
    @Enumerated(EnumType.STRING) private ChargeBasis chargeBasis;
    private String notes;

    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    private List<PropertyService> properties = new ArrayList<>();
}
