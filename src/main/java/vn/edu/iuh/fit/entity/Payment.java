package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


import vn.edu.iuh.fit.entity.enums.PaymentMethod;
import vn.edu.iuh.fit.entity.enums.PaymentStatus;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Payments")
public class Payment {
    @Id
    @Column(name = "payment_id", columnDefinition = "CHAR(36)")
    private String paymentId;

    @PrePersist
    void prePersist() {
        if (this.paymentId == null) this.paymentId = UUID.randomUUID().toString();
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", referencedColumnName = "booking_id")
    private Booking booking;

    @Column(name = "amount", precision = 12, scale = 2)
    private java.math.BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "refunded_amount", precision = 12, scale = 2)
    private java.math.BigDecimal refundedAmount;

    @Column(name = "created_at") private LocalDateTime createdAt;
}
