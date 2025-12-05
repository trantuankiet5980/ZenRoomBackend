package vn.edu.iuh.fit.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto {
    private String walletId;
    private BigDecimal balance;
    private LocalDateTime updatedAt;
}
