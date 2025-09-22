package vn.edu.iuh.fit.dtos.requests;

import lombok.Data;

@Data
public class PaymentWebhookPayload {
    private String invoiceId;
    private boolean success;
    private long amount;
    private String transactionId;
}
