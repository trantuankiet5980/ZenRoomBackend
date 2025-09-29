package vn.edu.iuh.fit.configs;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
@RequiredArgsConstructor
public class PayOSConfig {
    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private String clientId = dotenv.get("PAYOS_CLIENT_ID", "");
    private String apiKey = dotenv.get("PAYOS_API_KEY", "");
    private String checksumKey = dotenv.get("PAYOS_CHECKSUM_KEY", "");

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
