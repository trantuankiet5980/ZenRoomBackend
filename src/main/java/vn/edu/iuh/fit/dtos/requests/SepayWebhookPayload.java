package vn.edu.iuh.fit.dtos.requests;

import lombok.Data;

@Data
public class SepayWebhookPayload {
    private long id;                 // ID giao dịch trên SePay
    private String gateway;          // brand bank
    private String transactionDate;  // "yyyy-MM-dd HH:mm:ss"
    private String accountNumber;
    private String code;             // invoiceNo (nếu SePay parse được)
    private String content;          // nội dung ck (có thể chứa "INV-...")
    private String transferType;     // "in" | "out"
    private long transferAmount;     // số tiền
    private Long accumulated;        // số dư
    private String subAccount;
    private String referenceCode;    // mã tham chiếu SMS
    private String description;      // raw SMS
}
