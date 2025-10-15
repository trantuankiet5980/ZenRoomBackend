package vn.edu.iuh.fit.configs;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.context.properties.ConfigurationProperties;
import static org.springframework.util.StringUtils.hasText;

@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private String apiKey;

    private String baseUrl = "https://generativelanguage.googleapis.com";

    private String filterModel = "gemini-2.5-flash";
    private String answerModel = "gemini-2.5-flash";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String resolveApiKey() {
        if (hasText(apiKey)) {
            return apiKey.trim();
        }
        String dotEnvKey = dotenv.get("GEMINI_API_KEY");
        if (hasText(dotEnvKey)) {
            return dotEnvKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (hasText(envKey)) {
            return envKey.trim();
        }
        String dotEnvPropertyKey = dotenv.get("gemini.api-key");
        if (hasText(dotEnvPropertyKey)) {
            return dotEnvPropertyKey.trim();
        }
        String sysPropKey = System.getProperty("GEMINI_API_KEY");
        if (hasText(sysPropKey)) {
            return sysPropKey.trim();
        }
        String googleEnvKey = System.getenv("GOOGLE_API_KEY");
        if (hasText(googleEnvKey)) {
            return googleEnvKey.trim();
        }
        String googleDotEnvKey = dotenv.get("GOOGLE_API_KEY");
        if (hasText(googleDotEnvKey)) {
            return googleDotEnvKey.trim();
        }
        return null;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getFilterModel() {
        return filterModel;
    }

    public void setFilterModel(String filterModel) {
        this.filterModel = filterModel;
    }

    public String getAnswerModel() {
        return answerModel;
    }

    public void setAnswerModel(String answerModel) {
        this.answerModel = answerModel;
    }
}
