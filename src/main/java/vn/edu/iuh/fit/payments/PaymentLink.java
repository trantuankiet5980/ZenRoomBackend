package vn.edu.iuh.fit.payments;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentLink {
    private String checkoutUrl;
    private String qrPayload;
}
