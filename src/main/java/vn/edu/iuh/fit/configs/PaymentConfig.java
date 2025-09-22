package vn.edu.iuh.fit.configs;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import vn.edu.iuh.fit.payments.FakePaymentGateway;
import vn.edu.iuh.fit.payments.PaymentGateway;

@Configuration
@RequiredArgsConstructor
public class PaymentConfig {
    private final FakePaymentGateway fake;
    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private String provider = dotenv.get("PAYMENT_PROVIDER", "FAKE");

    @Bean
    @Primary
    public PaymentGateway paymentGateway() {
        return switch (provider.toUpperCase()) {
            case "FAKE" -> fake;
            // case "payos" -> new PayOSGateway(...);
            // case "sepay" -> new SepayGateway(...);
            default -> throw new IllegalStateException("Unsupported PAYMENT_PROVIDER: " + provider);
        };
    }
}
