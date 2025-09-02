package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import vn.edu.iuh.fit.entities.enums.DiscountCodeStatus;
import vn.edu.iuh.fit.entities.enums.DiscountType;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "DiscountCodes")
public class DiscountCode {
    @Id @Column(name="code_id", columnDefinition="CHAR(36)") String codeId;
    @PrePersist
    private void prePersist() {
        if (this.codeId == null) {
            this.codeId = UUID.randomUUID().toString();
        }
    }
    private String code;
    private String description;
    @Enumerated(EnumType.STRING) private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer usageLimit;
    private Integer usedCount;
    @Enumerated(EnumType.STRING) private DiscountCodeStatus status;
}
