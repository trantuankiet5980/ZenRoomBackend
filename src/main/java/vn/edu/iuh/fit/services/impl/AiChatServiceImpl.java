package vn.edu.iuh.fit.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.iuh.fit.configs.GeminiProperties;
import vn.edu.iuh.fit.dtos.chat.*;
import vn.edu.iuh.fit.entities.Address;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.PropertyMedia;
import vn.edu.iuh.fit.entities.enums.ApartmentCategory;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.entities.enums.PropertyStatus;
import vn.edu.iuh.fit.entities.enums.PropertyType;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.services.AiChatService;
import vn.edu.iuh.fit.services.ai.LocationAliasService;
import vn.edu.iuh.fit.services.ai.GeminiClient;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final GeminiClient geminiClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final PropertyRepository propertyRepository;
    private final LocationAliasService locationAliasService;

    @Override
    @Transactional(readOnly = true)
    public AiChatResponse chat(AIChatRequest request) {
        FilterExtraction extraction = extractFilters(request.message(), request.history());
        SanitizedFilters filters = sanitize(extraction);
        List<Property> candidates = search(filters, request.sanitizedLimit());
        List<ChatPropertyDto> results = candidates.stream()
                .map(this::toChatProperty)
                .toList();
        String reply = buildReply(request.message(), filters, results);
        ChatFilterDto filterDto = toDto(filters);
        return new AiChatResponse(reply, filterDto, results);
    }

    private ChatFilterDto toDto(SanitizedFilters filters) {
        return new ChatFilterDto(
                filters.provinceCode(),
                filters.provinceName(),
                filters.districtCode(),
                filters.districtName(),
                filters.priceMin() != null ? filters.priceMin().longValue() : null,
                filters.priceMax() != null ? filters.priceMax().longValue() : null,
                filters.areaMin() != null ? filters.areaMin().doubleValue() : null,
                filters.areaMax() != null ? filters.areaMax().doubleValue() : null,
                filters.capacityMin(),
                filters.bedroomsMin(),
                filters.bathroomsMin(),
                filters.propertyType() != null ? filters.propertyType().name() : null,
                filters.apartmentCategory() != null ? filters.apartmentCategory().name() : null,
                filters.keywords().isEmpty() ? null : filters.keywords()
        );
    }

    private ChatPropertyDto toChatProperty(Property property) {
        Address address = property.getAddress();
        String district = address != null && address.getDistrict() != null ? address.getDistrict().getName_with_type() : null;
        String province = address != null && address.getProvince() != null ? address.getProvince().getName_with_type() : null;
        String thumbnail =
                Optional.ofNullable(property.getMedia()).orElseGet(java.util.Collections::emptyList)
                        .stream()
                        .sorted(
                                Comparator.comparingInt((PropertyMedia m) -> Boolean.TRUE.equals(m.getIsCover()) ? 0 : 1)
                                        .thenComparing(m -> m.getSortOrder() == null ? Integer.MAX_VALUE : m.getSortOrder())
                        )
                        .map(m -> m.getPosterUrl() != null ? m.getPosterUrl() : m.getUrl())
                        .findFirst()
                        .orElse(null);
        return new ChatPropertyDto(
                property.getPropertyId(),
                property.getTitle(),
                property.getPrice(),
                property.getArea(),
                property.getCapacity(),
                district,
                province,
                address != null ? address.getAddressFull() : null,
                property.getPropertyType() != null ? property.getPropertyType().name() : null,
                property.getApartmentCategory() != null ? property.getApartmentCategory().name() : null,
                property.getBedrooms(),
                property.getBathrooms(),
                thumbnail
        );
    }

    private FilterExtraction extractFilters(String message, List<AIChatRequest.HistoryMessage> history) {
        ObjectNode body = objectMapper.createObjectNode();

        ObjectNode systemInstruction = body.putObject("systemInstruction");
        systemInstruction.put("role", "user");
        ArrayNode systemParts = systemInstruction.putArray("parts");
        String systemPrompt = "Bạn là công cụ trích xuất bộ lọc tìm phòng. " +
                "Tin nhắn người dùng có thể viết tắt hoặc không dấu. " +
                "Hãy phân tích và điền các trường JSON chính xác theo schema đã cho. " +
                "Giá '2tr' hiểu là 2000000 VND, có thể dùng ±10% nếu chỉ có giá mục tiêu. " +
                "Luôn trả về JSON hợp lệ. Không giải thích.";
        systemParts.addObject().put("text", systemPrompt);

        ArrayNode contents = body.putArray("contents");

        if (history != null) {
            for (AIChatRequest.HistoryMessage msg : history) {
                if (msg == null || msg.content() == null || msg.content().isBlank()) continue;
                ObjectNode contentNode = contents.addObject();
                contentNode.put("role", Objects.equals(msg.role(), "assistant") ? "model" : "user");
                contentNode.putArray("parts").addObject().put("text", msg.content());
            }
        }

        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", message);

        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        ObjectNode schema = generationConfig.putObject("responseSchema");
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("province_hint").put("type", "string").put("description", "Địa điểm cấp tỉnh/thành");
        props.putObject("district_hint").put("type", "string").put("description", "Địa điểm cấp quận/huyện");
        props.putObject("price_min").put("type", "integer").put("minimum", 0);
        props.putObject("price_max").put("type", "integer").put("minimum", 0);
        props.putObject("price_target").put("type", "integer").put("minimum", 0);
        props.putObject("area_min").put("type", "number").put("minimum", 0);
        props.putObject("area_max").put("type", "number").put("minimum", 0);
        props.putObject("capacity_min").put("type", "integer").put("minimum", 0);
        props.putObject("bedrooms_min").put("type", "integer").put("minimum", 0);
        props.putObject("bathrooms_min").put("type", "integer").put("minimum", 0);
        props.putObject("property_type").put("type", "string");
        props.putObject("apartment_category").put("type", "string");
        props.putObject("keywords").put("type", "array").putObject("items").put("type", "string");

        JsonNode response = geminiClient.generateContent(geminiProperties.getFilterModel(), body);
        String arguments = extractFirstText(response);
        if (arguments == null || arguments.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini không trả về dữ liệu bộ lọc");
        }
        try {
            JsonNode argsNode = objectMapper.readTree(arguments);
            return new FilterExtraction(
                    textValue(argsNode, "province_hint"),
                    textValue(argsNode, "district_hint"),
                    numberValue(argsNode, "price_min"),
                    numberValue(argsNode, "price_max"),
                    numberValue(argsNode, "price_target"),
                    decimalValue(argsNode, "area_min"),
                    decimalValue(argsNode, "area_max"),
                    intValue(argsNode, "capacity_min"),
                    intValue(argsNode, "bedrooms_min"),
                    intValue(argsNode, "bathrooms_min"),
                    textValue(argsNode, "property_type"),
                    textValue(argsNode, "apartment_category"),
                    listValue(argsNode, "keywords")
            );
        } catch (Exception e) {
            log.error("Cannot parse Gemini filter arguments: {}", arguments, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không đọc được dữ liệu từ Gemini", e);
        }
    }

    private SanitizedFilters sanitize(FilterExtraction extraction) {
        BigDecimal priceMin = toBigDecimal(extraction.priceMin());
        BigDecimal priceMax = toBigDecimal(extraction.priceMax());
        BigDecimal priceTarget = toBigDecimal(extraction.priceTarget());
        if (priceMin == null && priceMax == null && priceTarget != null) {
            BigDecimal delta = priceTarget.multiply(BigDecimal.valueOf(0.1));
            priceMin = priceTarget.subtract(delta).setScale(0, RoundingMode.DOWN);
            priceMax = priceTarget.add(delta).setScale(0, RoundingMode.UP);
        }
        if (priceMin != null && priceMax != null && priceMin.compareTo(priceMax) > 0) {
            BigDecimal tmp = priceMin;
            priceMin = priceMax;
            priceMax = tmp;
        }

        BigDecimal areaMin = extraction.areaMin() != null ? BigDecimal.valueOf(extraction.areaMin()) : null;
        BigDecimal areaMax = extraction.areaMax() != null ? BigDecimal.valueOf(extraction.areaMax()) : null;
        if (areaMin != null && areaMax != null && areaMin.compareTo(areaMax) > 0) {
            BigDecimal tmp = areaMin;
            areaMin = areaMax;
            areaMax = tmp;
        }

        LocationAliasService.LocationMatch location = locationAliasService
                .resolve(extraction.provinceHint(), extraction.districtHint())
                .orElse(null);

        PropertyType propertyType = normalizeEnum(extraction.propertyType(), PropertyType.class);
        ApartmentCategory apartmentCategory = normalizeEnum(extraction.apartmentCategory(), ApartmentCategory.class);

        List<String> keywords = extraction.keywords() != null
                ? extraction.keywords().stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();

        return new SanitizedFilters(
                location != null ? location.provinceCode() : null,
                location != null ? location.provinceName() : null,
                location != null ? location.districtCode() : null,
                location != null ? location.districtName() : null,
                priceMin,
                priceMax,
                areaMin,
                areaMax,
                extraction.capacityMin(),
                extraction.bedroomsMin(),
                extraction.bathroomsMin(),
                propertyType,
                apartmentCategory,
                keywords
        );
    }

    private List<Property> search(SanitizedFilters filters, int limit) {
        Specification<Property> spec = (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("address", JoinType.LEFT);
                root.fetch("media", JoinType.LEFT);
            }
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("postStatus"), PostStatus.APPROVED));
            predicates.add(cb.equal(root.get("status"), PropertyStatus.AVAILABLE));

            Join<Property, Address> addressJoin = null;
            if (filters.provinceCode() != null || filters.districtCode() != null) {
                addressJoin = root.join("address", JoinType.LEFT);
            }
            if (filters.provinceCode() != null && addressJoin != null) {
                predicates.add(cb.equal(addressJoin.join("province", JoinType.LEFT).get("code"), filters.provinceCode()));
            }
            if (filters.districtCode() != null && addressJoin != null) {
                predicates.add(cb.equal(addressJoin.join("district", JoinType.LEFT).get("code"), filters.districtCode()));
            }
            if (filters.priceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filters.priceMin()));
            }
            if (filters.priceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filters.priceMax()));
            }
            if (filters.areaMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), filters.areaMin()));
            }
            if (filters.areaMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), filters.areaMax()));
            }
            if (filters.capacityMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("capacity"), filters.capacityMin()));
            }
            if (filters.bedroomsMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), filters.bedroomsMin()));
            }
            if (filters.bathroomsMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bathrooms"), filters.bathroomsMin()));
            }
            if (filters.propertyType() != null) {
                predicates.add(cb.equal(root.get("propertyType"), filters.propertyType()));
            }
            if (filters.apartmentCategory() != null) {
                predicates.add(cb.equal(root.get("apartmentCategory"), filters.apartmentCategory()));
            }
            if (!filters.keywords().isEmpty()) {
                for (String keyword : filters.keywords()) {
                    String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("title")), pattern),
                            cb.like(cb.lower(root.get("description")), pattern)
                    ));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("createdAt"));
        return propertyRepository.findAll(spec, PageRequest.of(0, limit, sort)).getContent();
    }

    private String buildReply(String userMessage, SanitizedFilters filters, List<ChatPropertyDto> results) {
        ObjectNode body = objectMapper.createObjectNode();

        ObjectNode systemInstruction = body.putObject("systemInstruction");
        systemInstruction.put("role", "user");
        ArrayNode systemParts = systemInstruction.putArray("parts");
        String systemPrompt = "Bạn là trợ lý ZenRoom. Hãy trả lời bằng tiếng Việt, thân thiện và ngắn gọn. " +
                "Mở đầu hãy tóm tắt điều kiện chính (địa điểm, giá). " +
                "Sau đó liệt kê tối đa 5 phòng dạng bullet với tên, giá rút gọn (ví dụ 5.2tr/tháng), diện tích, sức chứa, khu vực. " +
                "Cuối cùng gợi ý cách tinh chỉnh thêm. Nếu không có phòng nào thì xin lỗi nhẹ nhàng và đề xuất thay đổi điều kiện.";
        systemParts.addObject().put("text", systemPrompt);

        ObjectNode context = objectMapper.createObjectNode();
        context.put("user_message", userMessage);
        ObjectNode filterNode = context.putObject("filters");
        put(filterNode, "province", filters.provinceName());
        put(filterNode, "district", filters.districtName());
        if (filters.priceMin() != null || filters.priceMax() != null) {
            ObjectNode priceNode = filterNode.putObject("price_vnd");
            if (filters.priceMin() != null) priceNode.put("min", filters.priceMin());
            if (filters.priceMax() != null) priceNode.put("max", filters.priceMax());
        }
        if (filters.areaMin() != null || filters.areaMax() != null) {
            ObjectNode areaNode = filterNode.putObject("area_m2");
            if (filters.areaMin() != null) areaNode.put("min", filters.areaMin());
            if (filters.areaMax() != null) areaNode.put("max", filters.areaMax());
        }
        put(filterNode, "propertyType", filters.propertyType() != null ? filters.propertyType().name() : null);
        if (!filters.keywords().isEmpty()) {
            ArrayNode kw = filterNode.putArray("keywords");
            filters.keywords().forEach(kw::add);
        }

        ArrayNode resultNode = context.putArray("results");
        for (ChatPropertyDto dto : results) {
            ObjectNode item = resultNode.addObject();
            item.put("id", dto.propertyId());
            item.put("title", dto.title());
            if (dto.price() != null) {
                item.put("price_vnd", dto.price());
                item.put("price_text", humanPrice(dto.price()));
            }
            if (dto.area() != null) item.put("area_m2", dto.area());
            if (dto.capacity() != null) item.put("capacity", dto.capacity());
            put(item, "district", dto.district());
            put(item, "province", dto.province());
            put(item, "address", dto.address());
            put(item, "propertyType", dto.propertyType());
            put(item, "apartmentCategory", dto.apartmentCategory());
            if (dto.bedrooms() != null) item.put("bedrooms", dto.bedrooms());
            if (dto.bathrooms() != null) item.put("bathrooms", dto.bathrooms());
            put(item, "thumbnail", dto.thumbnailUrl());
        }

        ArrayNode contents = body.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", context.toPrettyString());

        JsonNode response = geminiClient.generateContent(geminiProperties.getAnswerModel(), body);
        String text = extractFirstText(response);
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini không trả về câu trả lời");
        }
        return text;
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

    private void put(ObjectNode node, String field, String value) {
        if (value != null && !value.isBlank()) {
            node.put(field, value);
        }
    }

    private BigDecimal toBigDecimal(Number number) {
        if (number == null) return null;
        return BigDecimal.valueOf(number.longValue());
    }

    private Integer intValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value != null && value.isNumber()) {
            return value.intValue();
        }
        return null;
    }

    private Number numberValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value != null && value.isNumber()) {
            return value.numberValue();
        }
        return null;
    }

    private Double decimalValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value != null && value.isNumber()) {
            return value.doubleValue();
        }
        return null;
    }

    private String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value != null && value.isTextual()) {
            return value.asText();
        }
        return null;
    }

    private List<String> listValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value != null && value.isArray()) {
            List<String> list = new ArrayList<>();
            value.forEach(item -> {
                if (item.isTextual()) {
                    list.add(item.asText());
                }
            });
            return list;
        }
        return List.of();
    }

    private String humanPrice(BigDecimal price) {
        if (price == null) return null;
        BigDecimal million = price.divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.HALF_UP);
        return million.stripTrailingZeros().toPlainString() + "tr/đêm";
    }

    private <E extends Enum<E>> E normalizeEnum(String value, Class<E> enumType) {
        if (value == null || value.isBlank()) return null;
        String upper = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        try {
            return Enum.valueOf(enumType, upper);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private record FilterExtraction(
            String provinceHint,
            String districtHint,
            Number priceMin,
            Number priceMax,
            Number priceTarget,
            Double areaMin,
            Double areaMax,
            Integer capacityMin,
            Integer bedroomsMin,
            Integer bathroomsMin,
            String propertyType,
            String apartmentCategory,
            List<String> keywords
    ) {}

    private record SanitizedFilters(
            String provinceCode,
            String provinceName,
            String districtCode,
            String districtName,
            BigDecimal priceMin,
            BigDecimal priceMax,
            BigDecimal areaMin,
            BigDecimal areaMax,
            Integer capacityMin,
            Integer bedroomsMin,
            Integer bathroomsMin,
            PropertyType propertyType,
            ApartmentCategory apartmentCategory,
            List<String> keywords
    ) {}
}