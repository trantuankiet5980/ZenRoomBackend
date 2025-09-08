package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.entities.enums.ChargeBasis;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "contract_services",
        indexes = {
                @Index(name = "idx_contract_service_contract", columnList = "contract_id")
        })
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ContractService {
    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @PrePersist
    void pre() { if (id == null) id = UUID.randomUUID().toString(); }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "service_name", length = 100, nullable = false)
    private String serviceName;          // ví dụ: "Điện", "Nước", "Internet", "Dọn dẹp"

    @Column(name = "fee", precision = 12, scale = 2, nullable = false)
    private BigDecimal fee;              // ví dụ: 3,500 đ/kWh, hoặc 200,000 đ/tháng

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_basis", length = 30, nullable = false)
    private ChargeBasis chargeBasis;     // PER_KWH, PER_M3, PER_MONTH, PER_PERSON, FIXED, ...

    @Column(name = "is_included")
    private Boolean isIncluded;          // true: đã bao gồm trong giá thuê

    @Column(name = "note", length = 255)
    private String note;                 // mô tả thêm
}
