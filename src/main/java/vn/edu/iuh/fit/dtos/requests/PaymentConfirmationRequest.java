package vn.edu.iuh.fit.dtos.requests;

import lombok.Data;

@Data
public class PaymentConfirmationRequest {
    private String invoiceId;
    private Long amount;
    private String transactionId;
}
