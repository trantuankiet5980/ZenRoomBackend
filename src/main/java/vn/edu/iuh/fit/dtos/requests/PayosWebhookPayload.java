package vn.edu.iuh.fit.dtos.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayosWebhookPayload {
    private String code;
    private String desc;
    private PayosWebhookData data;
    private String signature;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayosWebhookData {
        private Long orderCode;
        private Long amount;
        private String description;
        private String status;
        private String transactionId;
        private String paymentLinkId;
    }
}
