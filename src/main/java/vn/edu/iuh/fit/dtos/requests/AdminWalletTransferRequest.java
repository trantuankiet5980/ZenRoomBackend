package vn.edu.iuh.fit.dtos.requests;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminWalletTransferRequest {
    private String userId;
    private BigDecimal amount;
    private String description;
}
