package vn.edu.iuh.fit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import vn.edu.iuh.fit.configs.GeminiProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(GeminiProperties.class)
public class ZenRoomBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZenRoomBackendApplication.class, args);
    }

}
