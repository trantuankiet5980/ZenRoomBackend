package vn.edu.iuh.fit.configs;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {
    private static final Logger log = LoggerFactory.getLogger(AwsS3Config.class);

    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private final String accessKey = dotenv.get("AWS_ACCESS_KEY_ID");
    private final String secretKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
    private final String region = dotenv.get("AWS_REGION", "ap-southeast-2");

    @PostConstruct
    void validate() {
        if (isBlank(accessKey) || isBlank(secretKey)) {
            throw new IllegalStateException("Missing AWS credentials. Check .env: AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY");
        }
        log.info("AWS S3 config OK. region={}", region);
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
