package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.SearchSuggestionDto;
import vn.edu.iuh.fit.entities.Address;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.SearchQueryLog;
import vn.edu.iuh.fit.entities.SearchSuggestion;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.entities.enums.PropertyStatus;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.SearchQueryLogRepository;
import vn.edu.iuh.fit.repositories.SearchSuggestionRepository;
import vn.edu.iuh.fit.services.SearchSuggestionService;
import vn.edu.iuh.fit.utils.TextNormalizer;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchSuggestionServiceImpl implements SearchSuggestionService {

    private static final String REFERENCE_TYPE_PROPERTY = "PROPERTY";
    private static final int MAX_FUZZY_CANDIDATES = 50;
    private static final int MAX_TOKEN_QUERIES = 4;
    private static final double CLICK_WEIGHT = 3.0d;
    private static final double QUERY_WEIGHT = 0.5d;

    private final SearchSuggestionRepository repository;
    private final PropertyRepository propertyRepository;
    private final SearchQueryLogRepository queryLogRepository;

    //chuẩn hóa chuỗi tìm kiếm: lowercase/bỏ dấu/loại ký tự lạ bằng TextNormalizer
    @Override
    @Transactional(readOnly = true)
    public List<SearchSuggestionDto> suggest(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        int resolvedLimit = limit <= 0 ? 10 : Math.min(limit, 20);
        String normalized = TextNormalizer.normalize(query);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        Map<String, ScoredSuggestion> aggregated = new LinkedHashMap<>();

        repository.findFirstByActiveTrueAndNormalizedText(normalized)
                .ifPresent(s -> addSuggestion(aggregated, s, normalized, MatchType.EXACT, now));

        Pageable prefixPage = PageRequest.of(0, resolvedLimit);
        List<SearchSuggestion> prefixMatches = repository
                .findByActiveTrueAndNormalizedTextStartingWith(normalized, prefixPage);
        prefixMatches.forEach(s -> addSuggestion(aggregated, s, normalized, MatchType.PREFIX, now));

        if (aggregated.size() < resolvedLimit) {
            List<String> tokens = extractQueryTokens(normalized);
            for (String token : tokens) {
                Pageable tokenPage = PageRequest.of(0, resolvedLimit);
                List<SearchSuggestion> tokenMatches = repository.findActiveByTermPrefix(token, tokenPage);
                tokenMatches.forEach(s -> addSuggestion(aggregated, s, normalized, MatchType.TOKEN, now));
                if (aggregated.size() >= resolvedLimit * 2) {
                    break;
                }
            }
        }

        if (aggregated.size() < resolvedLimit * 2) {
            Pageable containsPage = PageRequest.of(0, resolvedLimit * 2);
            List<SearchSuggestion> containsMatches = repository
                    .findByActiveTrueAndNormalizedTextContaining(normalized, containsPage);
            containsMatches.forEach(s -> addSuggestion(aggregated, s, normalized, MatchType.CONTAINS, now));
        }

        if (aggregated.size() < resolvedLimit * 3) {
            String token = normalized.length() <= 3 ? normalized : normalized.substring(0, 3);
            Pageable tokenPage = PageRequest.of(0, MAX_FUZZY_CANDIDATES);
            List<SearchSuggestion> fuzzyCandidates = repository.findActiveByToken(token, tokenPage);
            fuzzyCandidates.forEach(s -> addSuggestion(aggregated, s, normalized, MatchType.FUZZY, now));
        }

        return aggregated.values().stream()
                .sorted(Comparator.comparingDouble(ScoredSuggestion::score).reversed())
                .limit(resolvedLimit)
                .map(ScoredSuggestion::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void upsertPropertySuggestion(Property property) {
        if (property == null) {
            return;
        }
        boolean active = isPropertyActive(property);
        String referenceId = property.getPropertyId();
        SearchSuggestion suggestion = repository
                .findByReferenceTypeAndReferenceId(REFERENCE_TYPE_PROPERTY, referenceId)
                .orElseGet(SearchSuggestion::new);

        suggestion.setReferenceType(REFERENCE_TYPE_PROPERTY);
        suggestion.setReferenceId(referenceId);
        suggestion.setTitle(property.getTitle());
        suggestion.setSubtitle(buildSubtitle(property));
        suggestion.setPrice(property.getPrice());
        suggestion.setMetadata(buildMetadata(property));
        suggestion.setKeywords(buildKeywords(property));
        suggestion.setNormalizedText(buildNormalizedText(property));
        suggestion.setNormalizedTerms(buildNormalizedTerms(property));
        suggestion.setPopularityWeight(suggestion.getPopularityWeight() != null ? suggestion.getPopularityWeight() : 1.0d);
        suggestion.setActive(active);
        if (suggestion.getNormalizedTerms() == null || suggestion.getNormalizedTerms().isBlank()) {
            suggestion.setNormalizedTerms(" " + suggestion.getNormalizedText() + " ");
        }
        refreshPopularity(suggestion);

        repository.save(suggestion);
    }

    @Override
    @Transactional
    public void removePropertySuggestion(String propertyId) {
        if (propertyId == null || propertyId.isBlank()) {
            return;
        }
        repository.deleteByReferenceTypeAndReferenceId(REFERENCE_TYPE_PROPERTY, propertyId);
    }

    @Override
    @Transactional
    public void rebuildPropertySuggestions() {
        List<Property> properties = propertyRepository.findAll();
        Map<String, SearchSuggestion> existing = repository.findAll().stream()
                .filter(s -> REFERENCE_TYPE_PROPERTY.equals(s.getReferenceType()))
                .collect(Collectors.toMap(SearchSuggestion::getReferenceId, s -> s));

        Set<String> propertyIds = properties.stream()
                .map(Property::getPropertyId)
                .collect(Collectors.toSet());

        List<SearchSuggestion> updated = new ArrayList<>();
        for (Property property : properties) {
            SearchSuggestion suggestion = existing.getOrDefault(property.getPropertyId(), new SearchSuggestion());
            suggestion.setReferenceType(REFERENCE_TYPE_PROPERTY);
            suggestion.setReferenceId(property.getPropertyId());
            suggestion.setTitle(property.getTitle());
            suggestion.setSubtitle(buildSubtitle(property));
            suggestion.setPrice(property.getPrice());
            suggestion.setMetadata(buildMetadata(property));
            suggestion.setKeywords(buildKeywords(property));
            suggestion.setNormalizedText(buildNormalizedText(property));
            suggestion.setNormalizedTerms(buildNormalizedTerms(property));
            if (suggestion.getPopularityWeight() == null) {
                suggestion.setPopularityWeight(1.0d);
            }
            suggestion.setActive(isPropertyActive(property));
            if (suggestion.getNormalizedTerms() == null || suggestion.getNormalizedTerms().isBlank()) {
                suggestion.setNormalizedTerms(" " + suggestion.getNormalizedText() + " ");
            }
            refreshPopularity(suggestion);
            updated.add(suggestion);
        }
        repository.saveAll(updated);
        mergePopularityFromLogs(updated);

        List<SearchSuggestion> toRemove = existing.values().stream()
                .filter(s -> !propertyIds.contains(s.getReferenceId()))
                .toList();
        if (!toRemove.isEmpty()) {
            repository.deleteAll(toRemove);
        }
    }

    @Override
    @Transactional
    public void recordQuery(String query, String suggestionId) {
        if (query == null || query.isBlank()) {
            return;
        }
        String normalized = limitLength(TextNormalizer.normalize(query), 255);
        if (normalized.isEmpty()) {
            return;
        }

        if (suggestionId != null && !suggestionId.isBlank()) {
            SearchQueryLog log = queryLogRepository
                    .findBySuggestionIdAndNormalizedQuery(suggestionId, normalized)
                    .orElseGet(() -> SearchQueryLog.builder()
                            .suggestionId(suggestionId)
                            .normalizedQuery(normalized)
                            .rawQuery(query)
                            .build());
            log.incrementQuery(query);
            queryLogRepository.save(log);

            repository.findById(suggestionId).ifPresent(suggestion -> {
                suggestion.setQueryCount(suggestion.getQueryCount() + 1);
                suggestion.setLastInteractedAt(LocalDateTime.now());
                refreshPopularity(suggestion);
                repository.save(suggestion);
            });
        } else {
            SearchQueryLog log = queryLogRepository
                    .findByNormalizedQueryWithoutSuggestion(normalized)
                    .orElseGet(() -> SearchQueryLog.builder()
                            .normalizedQuery(normalized)
                            .rawQuery(query)
                            .build());
            log.incrementQuery(query);
            queryLogRepository.save(log);
        }
    }

    @Override
    @Transactional
    public void recordClick(String query, String suggestionId) {
        if (suggestionId == null || suggestionId.isBlank()) {
            return;
        }
        String normalized = limitLength(TextNormalizer.normalize(query), 255);
        if (normalized.isEmpty()) {
            normalized = limitLength(suggestionId, 255);
        }
        String finalNormalized = normalized;
        SearchQueryLog log = queryLogRepository
                .findBySuggestionIdAndNormalizedQuery(suggestionId, normalized)
                .orElseGet(() -> SearchQueryLog.builder()
                        .suggestionId(suggestionId)
                        .normalizedQuery(finalNormalized)
                        .rawQuery(query)
                        .build());
        log.incrementQuery(query);
        log.incrementClick(query);
        queryLogRepository.save(log);

        repository.findById(suggestionId).ifPresent(suggestion -> {
            suggestion.setQueryCount(suggestion.getQueryCount() + 1);
            suggestion.setClickCount(suggestion.getClickCount() + 1);
            suggestion.setLastInteractedAt(LocalDateTime.now());
            refreshPopularity(suggestion);
            repository.save(suggestion);
        });
    }

    private void addSuggestion(Map<String, ScoredSuggestion> aggregated,
                               SearchSuggestion suggestion,
                               String normalizedQuery,
                               MatchType matchType,
                               LocalDateTime now) {
        if (suggestion == null || suggestion.getNormalizedText() == null) {
            return;
        }
        String key = suggestion.getSuggestionId();
        if (key == null) {
            key = suggestion.getReferenceType() + ":" + suggestion.getReferenceId();
        }

        double score = computeScore(suggestion, normalizedQuery, matchType, now);
        if (Double.isInfinite(score) || Double.isNaN(score)) {
            return;
        }
        ScoredSuggestion existing = aggregated.get(key);
        if (existing == null || score > existing.score()) {
            aggregated.put(key, new ScoredSuggestion(suggestion, score));
        }
    }

    private double computeScore(SearchSuggestion suggestion,
                                String normalizedQuery,
                                MatchType matchType,
                                LocalDateTime now) {
        String normalizedTarget = suggestion.getNormalizedText();
        if (normalizedTarget == null || normalizedTarget.isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }
        if (normalizedTarget.length() > 120) {
            normalizedTarget = normalizedTarget.substring(0, 120);
        }
        double base = suggestion.getPopularityWeight() != null ? suggestion.getPopularityWeight() : 1.0d;
        base += suggestion.getClickCount() * CLICK_WEIGHT;
        base += suggestion.getQueryCount() * QUERY_WEIGHT;
        if (suggestion.getLastInteractedAt() != null) {
            long hours = ChronoUnit.HOURS.between(suggestion.getLastInteractedAt(), now);
            if (hours < 168) { // boost up to a week
                double recencyBoost = (168 - hours) / 168.0 * 10.0;
                base += Math.max(recencyBoost, 0.0);
            }
        }
        double score = base;

        switch (matchType) {
            case EXACT -> score += 140.0;
            case PREFIX -> score += 110.0;
            case TOKEN -> score += 85.0;
            case CONTAINS -> score += 60.0;
            case FUZZY -> score += 30.0;
        }

        int distance = levenshtein(normalizedQuery, normalizedTarget);
        if (matchType == MatchType.FUZZY) {
            int threshold = Math.max(2, Math.min(4, normalizedQuery.length() / 2 + 1));
            if (distance > threshold) {
                return Double.NEGATIVE_INFINITY;
            }
        }
        int maxLength = Math.max(normalizedQuery.length(), normalizedTarget.length());
        double similarity = maxLength == 0 ? 0.0 : 1.0 - ((double) distance / maxLength);
        score += similarity * 40.0;
        double trigram = trigramSimilarity(normalizedQuery, normalizedTarget);
        score += trigram * 30.0;

        return score;
    }

    private int levenshtein(String a, String b) {
        int lenA = a.length();
        int lenB = b.length();
        int[] prev = new int[lenB + 1];
        int[] curr = new int[lenB + 1];

        for (int j = 0; j <= lenB; j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= lenA; i++) {
            curr[0] = i;
            for (int j = 1; j <= lenB; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return prev[lenB];
    }

    private boolean isPropertyActive(Property property) {
        if (property.getPostStatus() != PostStatus.APPROVED) {
            return false;
        }
        PropertyStatus status = property.getStatus();
        return status == null || status == PropertyStatus.AVAILABLE;
    }

    private String buildSubtitle(Property property) {
        Address address = property.getAddress();
        String addressFull = address != null ? address.getAddressFull() : null;
        if (addressFull != null && !addressFull.isBlank()) {
            return addressFull;
        }
        if (address != null) {
            List<String> parts = new ArrayList<>();
            if (address.getStreet() != null) parts.add(address.getStreet());
            if (address.getWard() != null) parts.add(address.getWard().getName_with_type());
            if (address.getDistrict() != null) parts.add(address.getDistrict().getName_with_type());
            if (address.getProvince() != null) parts.add(address.getProvince().getName_with_type());
            return parts.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).collect(Collectors.joining(", "));
        }
        return null;
    }

    private String buildMetadata(Property property) {
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        List<String> metadata = new ArrayList<>();
        if (property.getPrice() != null) {
            metadata.add("Giá " + currencyFormat.format(property.getPrice()));
        }
        if (property.getArea() != null) {
            metadata.add("Diện tích " + property.getArea() + " m²");
        }
        if (property.getCapacity() != null) {
            metadata.add("Số người tối đa " + property.getCapacity());
        }
        return String.join(" • ", metadata);
    }

    private String buildKeywords(Property property) {
        List<String> keywords = new ArrayList<>();
        keywords.add(property.getTitle());
        keywords.add(property.getDescription());
        keywords.add(property.getBuildingName());
        keywords.add(property.getRoomNumber());
        if (property.getLandlord() != null) {
            keywords.add(property.getLandlord().getFullName());
        }
        Address address = property.getAddress();
        if (address != null) {
            keywords.add(address.getAddressFull());
            keywords.add(address.getStreet());
            if (address.getWard() != null) keywords.add(address.getWard().getName_with_type());
            if (address.getDistrict() != null) keywords.add(address.getDistrict().getName_with_type());
            if (address.getProvince() != null) keywords.add(address.getProvince().getName_with_type());
        }
        if (property.getPrice() != null) {
            keywords.add(formatNumeric(property.getPrice()));
        }
        return keywords.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(" | "));
    }

    private String buildNormalizedText(Property property) {
        String combined = gatherSearchableParts(property).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));

        String normalized = TextNormalizer.normalize(combined);
        if (normalized.isEmpty()) {
            normalized = TextNormalizer.normalize(
                    property.getTitle() != null ? property.getTitle() : property.getPropertyId());
        }
        return normalized;
    }

    private String buildNormalizedTerms(Property property) {
        List<String> parts = gatherSearchableParts(property);
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String normalizedPart = TextNormalizer.normalize(part);
            if (normalizedPart.isEmpty()) {
                continue;
            }
            for (String token : normalizedPart.split(" ")) {
                if (token.length() < 2) {
                    continue;
                }
                if (token.length() > 40) {
                    token = token.substring(0, 40);
                }
                tokens.add(token);
            }
        }
        if (tokens.isEmpty()) {
            return "";
        }
        String joined = String.join(" ", tokens);
        if (joined.length() > 500) {
            joined = joined.substring(0, 500).trim();
        }
        if (joined.isEmpty()) {
            return "";
        }
        return " " + joined + " ";
    }

    private List<String> gatherSearchableParts(Property property) {
        List<String> parts = new ArrayList<>();
        parts.add(property.getTitle());
        parts.add(property.getBuildingName());
        parts.add(property.getRoomNumber());
        parts.add(property.getDescription());
        if (property.getLandlord() != null) {
            parts.add(property.getLandlord().getFullName());
        }
        Address address = property.getAddress();
        if (address != null) {
            parts.add(address.getAddressFull());
            parts.add(address.getStreet());
            if (address.getWard() != null) parts.add(address.getWard().getName_with_type());
            if (address.getDistrict() != null) parts.add(address.getDistrict().getName_with_type());
            if (address.getProvince() != null) parts.add(address.getProvince().getName_with_type());
        }
        if (property.getPrice() != null) {
            parts.add(formatNumeric(property.getPrice()));
        }
        if (property.getArea() != null) {
            parts.add(property.getArea().toString());
        }
        if (property.getCapacity() != null) {
            parts.add(property.getCapacity().toString());
        }
        return parts;
    }

    private List<String> extractQueryTokens(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(normalizedQuery.split(" "))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .map(token -> token.length() > 40 ? token.substring(0, 40) : token)
                .distinct()
                .limit(MAX_TOKEN_QUERIES)
                .collect(Collectors.toList());
    }

    private void mergePopularityFromLogs(Collection<SearchSuggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return;
        }
        Map<String, SearchSuggestion> suggestionMap = suggestions.stream()
                .filter(s -> s.getSuggestionId() != null)
                .collect(Collectors.toMap(SearchSuggestion::getSuggestionId, s -> s, (left, right) -> left));
        if (suggestionMap.isEmpty()) {
            return;
        }
        List<SearchQueryLog> logs = queryLogRepository.findTop50BySuggestionIdIsNotNullOrderByLastOccurredAtDesc();
        for (SearchQueryLog log : logs) {
            SearchSuggestion suggestion = suggestionMap.get(log.getSuggestionId());
            if (suggestion == null) {
                continue;
            }
            if (log.getQueryCount() > suggestion.getQueryCount()) {
                suggestion.setQueryCount(log.getQueryCount());
            }
            if (log.getClickCount() > suggestion.getClickCount()) {
                suggestion.setClickCount(log.getClickCount());
            }
            if (log.getLastOccurredAt() != null) {
                suggestion.setLastInteractedAt(log.getLastOccurredAt());
            }
            refreshPopularity(suggestion);
        }
        repository.saveAll(suggestionMap.values());
    }

    private void refreshPopularity(SearchSuggestion suggestion) {
        double computed = 1.0d + suggestion.getClickCount() * CLICK_WEIGHT + suggestion.getQueryCount() * QUERY_WEIGHT;
        Double current = suggestion.getPopularityWeight();
        if (current != null) {
            computed = Math.max(computed, current);
        }
        suggestion.setPopularityWeight(computed);
    }

    private double trigramSimilarity(String query, String target) {
        Set<String> queryTrigrams = buildTrigrams(query);
        Set<String> targetTrigrams = buildTrigrams(target);
        if (queryTrigrams.isEmpty() || targetTrigrams.isEmpty()) {
            return 0.0;
        }
        long intersection = queryTrigrams.stream().filter(targetTrigrams::contains).count();
        long union = queryTrigrams.size() + targetTrigrams.size() - intersection;
        if (union <= 0) {
            return 0.0;
        }
        return (double) intersection / union;
    }

    private Set<String> buildTrigrams(String value) {
        if (value == null) {
            return Collections.emptySet();
        }
        String padded = "  " + value + "  ";
        Set<String> trigrams = new HashSet<>();
        for (int i = 0; i < padded.length() - 2; i++) {
            trigrams.add(padded.substring(i, i + 3));
        }
        return trigrams;
    }

    private String limitLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String formatNumeric(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private record ScoredSuggestion(SearchSuggestion suggestion, double score) {
        SearchSuggestionDto toDto() {
            return SearchSuggestionDto.builder()
                    .suggestionId(suggestion.getSuggestionId())
                    .referenceType(suggestion.getReferenceType())
                    .referenceId(suggestion.getReferenceId())
                    .title(suggestion.getTitle())
                    .subtitle(suggestion.getSubtitle())
                    .price(suggestion.getPrice())
                    .metadata(suggestion.getMetadata())
                    .keywords(suggestion.getKeywords())
                    .score(score)
                    .build();
        }
    }

    private enum MatchType {
        EXACT,
        PREFIX,
        TOKEN,
        CONTAINS,
        FUZZY
    }
}
