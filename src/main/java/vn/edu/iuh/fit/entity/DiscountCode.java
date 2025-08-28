package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;

import vn.edu.iuh.fit.entity.enums.DiscountStatus;
import vn.edu.iuh.fit.entity.enums.DiscountType;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "DiscountCodes")
public class DiscountCode {
    @Id
    @Column(name = "code_id", columnDefinition = "CHAR(36)")
    private String codeId;

    @PrePersist
    void prePersist() {
        if (this.codeId == null) this.codeId = UUID.randomUUID().toString();
    }

    @Column(name = "code", length = 50, unique = true)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 10)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private java.math.BigDecimal discountValue;

    private java.time.LocalDate validFrom;
    private java.time.LocalDate validTo;

    @Column(name = "usage_limit") private Integer usageLimit;
    @Column(name = "used_count") private Integer usedCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private DiscountStatus status;

    @OneToMany(mappedBy = "discountCode", fetch = FetchType.LAZY)
    @Builder.Default private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "code", fetch = FetchType.LAZY)
    @Builder.Default private List<DiscountCodeUsage> usages = new ArrayList<>();
}
