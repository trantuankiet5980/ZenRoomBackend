package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.entities.enums.WalletTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "WalletTransactions")
public class WalletTransaction {
    @Id
    @Column(name = "transaction_id", columnDefinition = "CHAR(36)")
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    private WalletTransactionType type;

    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (transactionId == null) transactionId = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
