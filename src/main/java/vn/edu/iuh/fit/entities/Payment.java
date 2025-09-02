package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;


import vn.edu.iuh.fit.entities.enums.PaymentMethod;
import vn.edu.iuh.fit.entities.enums.PaymentStatus;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Payments")
public class Payment {
    @Id @Column(name="payment_id", columnDefinition="CHAR(36)") String paymentId;
    @PrePersist
    private void prePersist() {
        if (this.paymentId == null) {
            this.paymentId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.refundedAmount == null) {
            this.refundedAmount = BigDecimal.ZERO;
        }
    }
    @ManyToOne @JoinColumn(name="booking_id") private Booking booking;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) private PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING) private PaymentStatus paymentStatus;
    private String transactionId;
    private BigDecimal refundedAmount;
    private LocalDateTime createdAt;
}
