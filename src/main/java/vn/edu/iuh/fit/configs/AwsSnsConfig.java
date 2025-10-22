package vn.edu.iuh.fit.configs;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class AwsSnsConfig {

    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private final String accessKey =dotenv.get("AWS_SNS_ACCESS_KEY_ID");
    private final String secretKey = dotenv.get("AWS_SNS_SECRET_ACCESS_KEY");
    private final String region = dotenv.get("AWS_REGION2");

    @Bean
    public SnsClient snsClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}