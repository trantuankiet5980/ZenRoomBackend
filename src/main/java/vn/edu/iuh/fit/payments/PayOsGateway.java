package vn.edu.iuh.fit.payments;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class PayOsGateway implements PaymentGateway{
    private final PayOS payOS;

    @Override
    public PaymentLink createPayment(
            String invoiceId,
            long amount,
            String description,
            String returnUrl,
            String notifyUrl // webhook config trên dashboard, tạm không cần gửi vào request
    ) {
        try {
            long orderCode = Long.parseLong(
                    String.valueOf(new Date().getTime()).substring(4, 13)
            );

            Long amountLong = amount;

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amountLong)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(returnUrl)
                    .build();

            CreatePaymentLinkResponse res =
                    payOS.paymentRequests().create(request);

            return new PaymentLink(
                    res.getCheckoutUrl(),
                    res.getQrCode(),
                    res.getOrderCode(),
                    res.getPaymentLinkId()
            );
        } catch (PayOSException e) {
            throw new RuntimeException("PayOS createPayment error: " + e.getMessage(), e);
        }
    }
}
