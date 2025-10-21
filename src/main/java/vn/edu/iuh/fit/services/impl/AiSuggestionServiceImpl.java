package vn.edu.iuh.fit.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.iuh.fit.configs.GeminiProperties;
import vn.edu.iuh.fit.dtos.chat.AiSuggestionRequest;
import vn.edu.iuh.fit.dtos.chat.AiSuggestionResponse;
import vn.edu.iuh.fit.entities.Address;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.PropertyFurnishing;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.UserEventRepository;
import vn.edu.iuh.fit.services.AiSuggestionService;
import vn.edu.iuh.fit.services.ai.GeminiClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSuggestionServiceImpl implements AiSuggestionService {

    private static final int CONTEXT_SAMPLE_SIZE = 5;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    private final GeminiClient geminiClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final UserEventRepository userEventRepository;
    private final PropertyRepository propertyRepository;

    @Override
    @Transactional(readOnly = true)
    public AiSuggestionResponse suggest(AiSuggestionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        int limit = request.sanitizedLimit();
        SuggestionContext context = buildContext(request);

        try {
            List<String> generated = generateWithGemini(request, context, limit);
            List<AiSuggestionResponse.SuggestionItem> items = generated.stream()
                    .filter(text -> text != null && !text.isBlank())
                    .map(text -> new AiSuggestionResponse.SuggestionItem(text.trim()))
                    .limit(limit)
                    .toList();
            if (!items.isEmpty()) {
                return new AiSuggestionResponse(items);
            }
        } catch (ResponseStatusException ex) {
            log.warn("Gemini suggestions failed: {}", ex.getReason());
        } catch (Exception ex) {
            log.error("Unexpected error when generating AI suggestions", ex);
        }

        List<AiSuggestionResponse.SuggestionItem> fallback = buildFallbackSuggestions(request, context, limit);
        return new AiSuggestionResponse(fallback);
    }

    private List<String> generateWithGemini(AiSuggestionRequest request, SuggestionContext context, int limit) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode systemInstruction = body.putObject("systemInstruction");
        systemInstruction.put("role", "user");
        ArrayNode systemParts = systemInstruction.putArray("parts");
        systemParts.addObject().put("text", "Bạn là trợ lý gợi ý phòng nghỉ cho ứng dụng ZenRoom. " +
                "Luôn trả lời bằng tiếng Việt. " +
                "Sinh ra các gợi ý rất ngắn (tối đa 18 từ), nhấn mạnh vị trí, khoảng giá và nội thất. " +
                "Tập trung vào nhu cầu thực tế của người dùng.");

        ArrayNode contents = body.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", buildPrompt(request, context, limit));

        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        ObjectNode schema = generationConfig.putObject("responseSchema");
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode suggestionsNode = props.putObject("suggestions");
        suggestionsNode.put("type", "array");
        ObjectNode items = suggestionsNode.putObject("items");
        items.put("type", "object");
        items.putObject("properties").putObject("text").put("type", "string");

        ArrayNode required = schema.putArray("required");
        required.add("suggestions");

        String model = StringUtils.hasText(geminiProperties.getSuggestionModel())
                ? geminiProperties.getSuggestionModel()
                : geminiProperties.getAnswerModel();
        JsonNode response = geminiClient.generateContent(model, body);
        String payload = extractFirstText(response);
        if (!StringUtils.hasText(payload)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini không trả về dữ liệu gợi ý");
        }
        JsonNode root = objectMapper.readTree(payload);
        JsonNode suggestions = root.get("suggestions");
        if (suggestions == null || !suggestions.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini không trả về danh sách gợi ý hợp lệ");
        }
        List<String> result = new ArrayList<>();
        for (JsonNode node : suggestions) {
            if (node == null || !node.isObject()) continue;
            JsonNode textNode = node.get("text");
            if (textNode != null && textNode.isTextual()) {
                String text = textNode.asText();
                if (StringUtils.hasText(text)) {
                    result.add(text.trim());
                }
            }
        }
        return result.stream().limit(limit).toList();
    }

    private String buildPrompt(AiSuggestionRequest request, SuggestionContext context, int limit) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tạo ").append(limit).append(" gợi ý chỗ ở ngắn gọn. Mỗi gợi ý nên kết hợp địa điểm, ngân sách và nội thất.\n");
        prompt.append("Trả về JSON theo schema có sẵn.\n");

        if (StringUtils.hasText(request.userId())) {
            prompt.append("User ID: ").append(request.userId()).append('\n');
        }
        if (StringUtils.hasText(request.city())) {
            prompt.append("Thành phố ưu tiên: ").append(request.city()).append('\n');
        }
        if (StringUtils.hasText(request.district())) {
            prompt.append("Khu vực mong muốn: ").append(request.district()).append('\n');
        }
        if (request.budgetMin() != null || request.budgetMax() != null) {
            prompt.append("Ngân sách: ");
            if (request.budgetMin() != null) {
                prompt.append(">= ").append(formatMoney(request.budgetMin()));
            }
            if (request.budgetMin() != null && request.budgetMax() != null) {
                prompt.append(" - ");
            }
            if (request.budgetMax() != null) {
                prompt.append("<= ").append(formatMoney(request.budgetMax()));
            }
            prompt.append('\n');
        }
        if (request.checkInDate() != null || request.checkOutDate() != null) {
            prompt.append("Thời gian dự kiến: ");
            prompt.append(formatDateRange(request.checkInDate(), request.checkOutDate()));
            prompt.append('\n');
        }
        if (!request.safeFurnishingPriorities().isEmpty()) {
            prompt.append("Ưu tiên nội thất: ")
                    .append(String.join(", ", request.safeFurnishingPriorities()))
                    .append('\n');
        }

        if (!context.personal().isEmpty()) {
            prompt.append("Phòng người dùng đã quan tâm:\n");
            for (Property property : context.personal()) {
                prompt.append("- ").append(describePropertyForPrompt(property)).append('\n');
            }
        }
        if (!context.trending().isEmpty()) {
            prompt.append("Phòng đang thịnh hành 14 ngày qua:\n");
            for (Property property : context.trending()) {
                prompt.append("- ").append(describePropertyForPrompt(property)).append('\n');
            }
        }

        prompt.append("Ưu tiên những gợi ý khác nhau để người dùng có thêm lựa chọn.");
        return prompt.toString();
    }

    private List<AiSuggestionResponse.SuggestionItem> buildFallbackSuggestions(AiSuggestionRequest request,
                                                                               SuggestionContext context,
                                                                               int limit) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        for (Property property : context.personal()) {
            if (suggestions.size() >= limit) break;
            suggestions.add(buildPropertySuggestion(property, request, true));
        }
        for (Property property : context.trending()) {
            if (suggestions.size() >= limit) break;
            suggestions.add(buildPropertySuggestion(property, request, false));
        }
        if (suggestions.size() < limit) {
            suggestions.add(buildGenericSuggestion(request));
        }
        if (suggestions.size() < limit) {
            suggestions.add(buildWeekendDealSuggestion(request));
        }
        return suggestions.stream()
                .filter(text -> text != null && !text.isBlank())
                .limit(limit)
                .map(text -> new AiSuggestionResponse.SuggestionItem(text))
                .toList();
    }

    private SuggestionContext buildContext(AiSuggestionRequest request) {
        List<Property> personal = new ArrayList<>();
        LinkedHashSet<String> personalIds = new LinkedHashSet<>();
        if (StringUtils.hasText(request.userId())) {
            userEventRepository.findUserPropertyScores(request.userId(), PageRequest.of(0, CONTEXT_SAMPLE_SIZE))
                    .forEach(score -> {
                        String propertyId = score.getPropertyId();
                        if (personalIds.add(propertyId)) {
                            loadProperty(propertyId).ifPresent(personal::add);
                        }
                    });
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(14);
        List<Property> trending = new ArrayList<>();
        LinkedHashSet<String> trendingIds = new LinkedHashSet<>();
        userEventRepository.findPopularSince(threshold, PageRequest.of(0, CONTEXT_SAMPLE_SIZE))
                .forEach(score -> {
                    String propertyId = score.getPropertyId();
                    if (personalIds.contains(propertyId) || !trendingIds.add(propertyId)) {
                        return;
                    }
                    loadProperty(propertyId).ifPresent(trending::add);
                });

        return new SuggestionContext(personal, trending);
    }

    private Optional<Property> loadProperty(String propertyId) {
        if (!StringUtils.hasText(propertyId)) {
            return Optional.empty();
        }
        try {
            return propertyRepository.findWithDetailsByPropertyId(propertyId);
        } catch (Exception ex) {
            log.warn("Không tải được thông tin phòng {}", propertyId, ex);
            return Optional.empty();
        }
    }

    private String describePropertyForPrompt(Property property) {
        StringBuilder sb = new StringBuilder();
        sb.append(property.getTitle());
        String location = buildLocation(property);
        if (StringUtils.hasText(location)) {
            sb.append(" | ").append(location);
        }
        if (property.getPrice() != null) {
            sb.append(" | giá ").append(formatMoney(property.getPrice()));
        }
        String interior = describeInterior(property);
        if (StringUtils.hasText(interior)) {
            sb.append(" | ").append(interior);
        }
        return sb.toString();
    }

    private String buildPropertySuggestion(Property property, AiSuggestionRequest request, boolean personal) {
        if (property == null) {
            return null;
        }
        String location = buildLocation(property);
        if (!StringUtils.hasText(location)) {
            location = StringUtils.hasText(request.city()) ? request.city() : "trung tâm";
        }
        String interior = describeInterior(property);
        String price = property.getPrice() != null ? formatMoney(property.getPrice()) :
                (request.budgetMax() != null ? formatMoney(request.budgetMax()) : null);

        StringBuilder sb = new StringBuilder();
        if (personal) {
            sb.append("Phòng ");
        } else {
            sb.append("Hot ");
        }
        sb.append(location);
        if (StringUtils.hasText(interior)) {
            sb.append(' ').append(interior);
        } else {
            sb.append(" nội thất gọn gàng");
        }
        if (StringUtils.hasText(price)) {
            sb.append(" khoảng ").append(price);
        }
        return sb.toString();
    }

    private String buildGenericSuggestion(AiSuggestionRequest request) {
        String location = StringUtils.hasText(request.city()) ? request.city() : "trung tâm";
        String interior = interiorPreferencePhrase(request);
        if (request.budgetMax() != null) {
            return "Phòng " + location + ' ' + interior + " dưới " + formatMoney(request.budgetMax());
        }
        if (request.budgetMin() != null) {
            return "Phòng " + location + ' ' + interior + " từ " + formatMoney(request.budgetMin());
        }
        return "Phòng " + location + ' ' + interior + " giá tốt";
    }

    private String buildWeekendDealSuggestion(AiSuggestionRequest request) {
        String location = StringUtils.hasText(request.city()) ? request.city() : "trung tâm";
        String interior = interiorPreferencePhrase(request);
        StringBuilder sb = new StringBuilder("Deal cuối tuần phòng ");
        sb.append(location).append(' ').append(interior);
        if (request.budgetMax() != null) {
            sb.append(" dưới ").append(formatMoney(request.budgetMax()));
        } else if (request.budgetMin() != null) {
            sb.append(" khoảng ").append(formatMoney(request.budgetMin()));
        } else {
            sb.append(" giá mềm");
        }
        return sb.toString();
    }

    private String interiorPreferencePhrase(AiSuggestionRequest request) {
        List<String> priorities = request.safeFurnishingPriorities().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        if (priorities.isEmpty()) {
            return "full nội thất";
        }
        if (priorities.size() == 1) {
            return "có " + priorities.get(0);
        }
        return "có " + String.join(", ", priorities.subList(0, Math.min(2, priorities.size())));
    }

    private String describeInterior(Property property) {
        List<PropertyFurnishing> furnishings = property.getFurnishings();
        if (furnishings == null || furnishings.isEmpty()) {
            return null;
        }
        List<String> names = furnishings.stream()
                .map(PropertyFurnishing::getFurnishing)
                .filter(Objects::nonNull)
                .map(f -> StringUtils.hasText(f.getFurnishingName()) ? f.getFurnishingName().trim() : null)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(3)
                .toList();
        if (names.isEmpty()) {
            return null;
        }
        if (names.size() >= 3) {
            return "full nội thất";
        }
        return "có " + String.join(", ", names);
    }

    private String buildLocation(Property property) {
        Address address = property.getAddress();
        if (address == null) {
            return null;
        }
        if (address.getDistrict() != null && StringUtils.hasText(address.getDistrict().getName_with_type())) {
            return address.getDistrict().getName_with_type();
        }
        if (address.getProvince() != null && StringUtils.hasText(address.getProvince().getName_with_type())) {
            return address.getProvince().getName_with_type();
        }
        return address.getAddressFull();
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal million = BigDecimal.valueOf(1_000_000L);
        BigDecimal thousand = BigDecimal.valueOf(1_000L);
        if (value.compareTo(million) >= 0) {
            BigDecimal amount = value.divide(million, 1, RoundingMode.HALF_UP);
            return stripTrailingZeros(amount) + " triệu";
        }
        if (value.compareTo(thousand) >= 0) {
            BigDecimal amount = value.divide(thousand, 0, RoundingMode.DOWN);
            return stripTrailingZeros(amount) + "k";
        }
        return stripTrailingZeros(value) + "đ";
    }

    private String stripTrailingZeros(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() <= 0 ? normalized.toPlainString() : normalized.toString();
    }

    private String formatDateRange(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "chưa xác định";
        }
        if (start != null && end != null) {
            return DATE_FORMAT.format(start) + " - " + DATE_FORMAT.format(end);
        }
        return DATE_FORMAT.format(start != null ? start : end);
    }

    private String extractFirstText(JsonNode response) {
        if (response == null) {
            return null;
        }
        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return null;
        }
        for (JsonNode candidate : candidates) {
            if (candidate == null || candidate.isNull()) {
                continue;
            }
            JsonNode content = candidate.path("content");
            if (!content.isObject()) {
                continue;
            }
            JsonNode parts = content.path("parts");
            if (!parts.isArray()) {
                continue;
            }
            for (JsonNode part : parts) {
                JsonNode textNode = part != null ? part.get("text") : null;
                if (textNode != null && textNode.isTextual()) {
                    return textNode.asText();
                }
            }
        }
        return null;
    }

    private record SuggestionContext(List<Property> personal, List<Property> trending) {
        private SuggestionContext {
            personal = personal == null ? List.of() : List.copyOf(personal);
            trending = trending == null ? List.of() : List.copyOf(trending);
        }
    }
}
