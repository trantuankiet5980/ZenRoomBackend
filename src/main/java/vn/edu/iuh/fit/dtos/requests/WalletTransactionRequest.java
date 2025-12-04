package vn.edu.iuh.fit.dtos.requests;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletTransactionRequest {
    private BigDecimal amount;
    private String description;
}
