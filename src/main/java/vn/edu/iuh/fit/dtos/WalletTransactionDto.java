package vn.edu.iuh.fit.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.entities.enums.WalletTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionDto {
    private String transactionId;
    private WalletTransactionType type;
    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;
}
