package vn.edu.iuh.fit.configs;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GoogleMapsConfig {
    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private final String apiKey=dotenv.get("google.maps.api.key");

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getApiKey() {
        return apiKey;
    }
}
