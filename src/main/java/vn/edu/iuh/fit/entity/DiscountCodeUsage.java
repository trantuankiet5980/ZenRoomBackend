package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "DiscountCodeUsages")
public class DiscountCodeUsage {
    @Id
    @Column(name = "usage_id", columnDefinition = "CHAR(36)")
    private String usageId;

    @PrePersist
    void prePersist() {
        if (this.usageId == null) this.usageId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_id", referencedColumnName = "code_id")
    private DiscountCode code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", referencedColumnName = "booking_id")
    private Booking booking;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
