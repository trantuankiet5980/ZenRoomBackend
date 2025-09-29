package vn.edu.iuh.fit.payments;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class PayOsGateway implements PaymentGateway{
    private final PayOS payOS;

    @Override
    public PaymentLink createPayment(String invoiceId, long amount, String description, String returnUrl, String notifyUrl) {
        try {
            String now = String.valueOf(new Date().getTime());
            long orderCode = Long.parseLong(now.substring(now.length() - 6));

            ItemData item = ItemData.builder()
                    .name("Invoice " + invoiceId)
                    .price((int) amount)
                    .quantity(1)
                    .build();

            PaymentData pd = PaymentData.builder()
                    .orderCode(orderCode)
                    .description(description)
                    .amount((int) amount)
                    .item(item)
                    .returnUrl(returnUrl)
                    .cancelUrl(returnUrl)
                    .build();

            CheckoutResponseData data = payOS.createPaymentLink(pd);
            return new PaymentLink(data.getCheckoutUrl(), data.getQrCode(), data.getOrderCode(), data.getPaymentLinkId());
        } catch (Exception e) {
            throw new RuntimeException("PayOS createPayment error: " + e.getMessage(), e);
        }
    }
}
