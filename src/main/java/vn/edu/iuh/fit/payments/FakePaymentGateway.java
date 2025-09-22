package vn.edu.iuh.fit.payments;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component("fakeGateway")
public class FakePaymentGateway implements PaymentGateway{
    private final ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<>();

    @Override
    public String createPayment(String invoiceId, long amount, String description, String returnUrl, String notifyUrl) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, invoiceId);
        return "/api/v1/payments/fake/pay"
                + "?invoiceId=" + enc(invoiceId)
                + "&token=" + enc(token)
                + "&returnUrl=" + enc(nullToEmpty(returnUrl))
                + "&notifyUrl=" + enc(nullToEmpty(notifyUrl));
    }

    public String consumeToken(String token) { return tokens.remove(token); }

    private String enc(String s){ return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private String nullToEmpty(String s){ return s == null ? "" : s; }
}
