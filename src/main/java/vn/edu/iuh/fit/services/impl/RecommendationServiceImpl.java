package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.enums.EventType;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.entities.enums.PropertyType;
import vn.edu.iuh.fit.mappers.PropertyMapper;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.UserEventRepository;
import vn.edu.iuh.fit.services.RecommendationService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final int MAX_LIMIT = 50;
    private static final int HISTORY_SIZE = 10;
    private static final Duration POPULAR_WINDOW = Duration.ofDays(14);
    private static final Duration COVISIT_WINDOW = Duration.ofDays(30);
    private static final List<EventType> SIGNAL_EVENTS = List.of(EventType.VIEW, EventType.CLICK, EventType.FAVORITE, EventType.BOOKING);

    private final PropertyRepository propertyRepository;
    private final UserEventRepository userEventRepository;
    private final PropertyMapper propertyMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PropertyDto> getSimilarRooms(String roomId, int limit, String userId) {
        int sanitizedLimit = sanitizeLimit(limit);
        LinkedHashMap<String, Double> ranked = fetchSimilarCandidates(roomId, sanitizedLimit);

        if (ranked.isEmpty()) {
            return Collections.emptyList();
        }

        return toDtos(ranked, sanitizedLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyDto> getPersonalRecommendations(String userId, int limit) {
        int sanitizedLimit = sanitizeLimit(limit);
        LinkedHashMap<String, Double> scores = new LinkedHashMap<>();

        UserPreference preference = buildUserPreference(userId);
        if (!preference.isEmpty()) {
            for (String propertyId : preference.seedPropertyIds()) {
                double weight = preference.getPropertyWeight(propertyId);
                if (weight <= 0d) continue;
                try {
                    LinkedHashMap<String, Double> candidates = fetchSimilarCandidates(propertyId, sanitizedLimit);
                    mergeScores(scores, candidates, weight * 0.6);
                } catch (EntityNotFoundException ignored) {
                    continue;
                }

                LinkedHashMap<String, Double> covisit = fetchCoVisitationCandidates(propertyId, sanitizedLimit);
                mergeScores(scores, covisit, weight * 0.3);

                scores.remove(propertyId);
            }
        }

        LocalDateTime popularityThreshold = LocalDateTime.now().minus(POPULAR_WINDOW);
        List<UserEventRepository.PropertyScore> popular = userEventRepository
                .findPopularSince(popularityThreshold, PageRequest.of(0, sanitizedLimit));
        LinkedHashMap<String, Double> popularScores = popular.stream()
                .collect(Collectors.toMap(UserEventRepository.PropertyScore::getPropertyId,
                        UserEventRepository.PropertyScore::getScore,
                        Double::sum, LinkedHashMap::new));
        mergeScores(scores, popularScores, 0.25);

        if (scores.isEmpty()) {
            // Fallback to latest approved listings if user is new or we have no signals
            List<Property> fallback = propertyRepository.findByPostStatusOrderByPublishedAtDesc(
                    PostStatus.APPROVED, PageRequest.of(0, sanitizedLimit));
            fallback.stream()
                    .map(Property::getPropertyId)
                    .forEach(id -> scores.putIfAbsent(id, 1d));
        }

        // Re-apply personalized preference boosts before mapping to DTOs
        Map<String, Double> enriched = new HashMap<>(scores);
        if (!preference.isEmpty()) {
            List<Property> loaded = propertyRepository.findAllById(scores.keySet());
            Map<String, Property> byId = loaded.stream().collect(Collectors.toMap(Property::getPropertyId, p -> p));
            enriched.replaceAll((id, baseScore) -> {
                Property property = byId.get(id);
                if (property == null) return baseScore;
                double boost = preference.typeAffinity(property) * 0.2
                        + preference.priceAffinity(property) * 0.15
                        + preference.areaAffinity(property) * 0.1;
                return baseScore + boost;
            });
        }

        return toDtos(enriched, sanitizedLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyDto> rerankAfterSearch(String query, int limit, String userId) {
        int sanitizedLimit = sanitizeLimit(limit);
        if (query == null || query.isBlank()) {
            return getPersonalRecommendations(userId, sanitizedLimit);
        }
        Pageable page = PageRequest.of(0, sanitizedLimit * 2);
        List<Property> initial = propertyRepository.searchByKeyword(query, page);
        if (initial.isEmpty()) {
            return Collections.emptyList();
        }

        UserPreference preference = buildUserPreference(userId);
        LinkedHashMap<String, Double> scored = new LinkedHashMap<>();
        int rank = 1;
        for (Property property : initial) {
            double baseScore = 1.0 / rank;
            double total = baseScore;
            if (!preference.isEmpty()) {
                total += preference.getPropertyWeight(property.getPropertyId()) * 0.4
                        + preference.typeAffinity(property) * 0.25
                        + preference.priceAffinity(property) * 0.2
                        + preference.areaAffinity(property) * 0.1;
            }
            scored.put(property.getPropertyId(), total);
            rank++;
        }

        return toDtos(scored, sanitizedLimit);
    }

    private LinkedHashMap<String, Double> fetchSimilarCandidates(String roomId, int limit) {
        Property base = propertyRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found: " + roomId));

        LinkedHashMap<String, Double> scored = propertyRepository
                .findSimilarByEmbedding(roomId, limit)
                .stream()
                .collect(Collectors.toMap(Property::getPropertyId, p -> 1d, Double::sum, LinkedHashMap::new));

        if (scored.size() >= limit) {
            return scored;
        }

        List<Property> candidates = propertyRepository
                .findTop200ByPostStatusAndPropertyTypeAndPropertyIdNotOrderByCreatedAtDesc(
                        PostStatus.APPROVED,
                        base.getPropertyType(),
                        roomId
                );

        List<Map.Entry<String, Double>> weighted = candidates.stream()
                .map(candidate -> Map.entry(candidate.getPropertyId(), computeSimilarityScore(base, candidate)))
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .toList();

        for (Map.Entry<String, Double> entry : weighted) {
            if (entry.getValue() <= 0d) continue;
            scored.putIfAbsent(entry.getKey(), entry.getValue());
            if (scored.size() >= limit) break;
        }

        return scored;
    }

    private LinkedHashMap<String, Double> fetchCoVisitationCandidates(String roomId, int limit) {
        LocalDateTime threshold = LocalDateTime.now().minus(COVISIT_WINDOW);
        List<UserEventRepository.PropertyScore> covisit = userEventRepository
                .findCoVisitedProperties(roomId, SIGNAL_EVENTS, threshold, PageRequest.of(0, limit));
        return covisit.stream()
                .collect(Collectors.toMap(UserEventRepository.PropertyScore::getPropertyId,
                        UserEventRepository.PropertyScore::getScore,
                        Double::sum, LinkedHashMap::new));
    }

    private UserPreference buildUserPreference(String userId) {
        UserPreference preference = new UserPreference();
        if (userId == null || userId.isBlank()) {
            return preference;
        }

        List<UserEventRepository.UserPropertyScore> userScores = userEventRepository
                .findUserPropertyScores(userId, PageRequest.of(0, HISTORY_SIZE));
        if (userScores.isEmpty()) {
            return preference;
        }

        List<String> propertyIds = userScores.stream()
                .map(UserEventRepository.PropertyScore::getPropertyId)
                .toList();
        Map<String, Property> properties = propertyRepository.findAllById(propertyIds).stream()
                .collect(Collectors.toMap(Property::getPropertyId, p -> p));

        for (UserEventRepository.UserPropertyScore score : userScores) {
            Property property = properties.get(score.getPropertyId());
            if (property == null) continue;
            double weight = Math.max(0.1, score.getScore());
            preference.absorb(property, weight);
        }

        preference.normalize();
        return preference;
    }

    private void mergeScores(Map<String, Double> target, Map<String, Double> source, double weight) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((id, score) -> target.merge(id, score * weight, Double::sum));
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private List<PropertyDto> toDtos(Map<String, Double> scored, int limit) {
        List<String> orderedIds = scored.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .distinct()
                .limit(limit)
                .toList();

        if (orderedIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Property> properties = propertyRepository.findAllById(orderedIds);
        Map<String, Property> byId = properties.stream()
                .collect(Collectors.toMap(Property::getPropertyId, p -> p));

        List<PropertyDto> dtos = new ArrayList<>();
        for (String id : orderedIds) {
            Property property = byId.get(id);
            if (property != null) {
                dtos.add(propertyMapper.toDto(property));
            }
        }
        return dtos;
    }

    private double computeSimilarityScore(Property base, Property candidate) {
        WeightedScore score = new WeightedScore();

        score.add(0.25, base.getPropertyType() == candidate.getPropertyType() ? 1.0 : 0.0);
        score.addIfPresent(0.2, similarity(base.getPrice(), candidate.getPrice()));
        score.addIfPresent(0.15, similarity(base.getArea(), candidate.getArea()));
        score.addIfPresent(0.1, similarity(base.getDeposit(), candidate.getDeposit()));
        score.addIfPresent(0.1, similarity(base.getBedrooms(), candidate.getBedrooms()));
        score.addIfPresent(0.1, similarity(base.getBathrooms(), candidate.getBathrooms()));
        score.addIfPresent(0.1, similarity(base.getCapacity(), candidate.getCapacity()));

        if (base.getPropertyType() == PropertyType.BUILDING) {
            score.addIfPresent(0.1, categorySimilarity(base.getApartmentCategory(), candidate.getApartmentCategory()));
        }

        return score.finish();
    }

    private static double similarity(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return WeightedScore.SKIPPED_VALUE;
        return similarity(a.doubleValue(), b.doubleValue());
    }

    private static double similarity(Number a, Number b) {
        if (a == null || b == null) return WeightedScore.SKIPPED_VALUE;
        return similarity(a.doubleValue(), b.doubleValue());
    }

    private static double similarity(double a, double b) {
        double max = Math.max(Math.abs(a), Math.abs(b));
        if (max == 0d) return 1d;
        double diff = Math.abs(a - b);
        return Math.max(0d, 1d - (diff / max));
    }

    private static double categorySimilarity(Enum<?> a, Enum<?> b) {
        if (a == null || b == null) return WeightedScore.SKIPPED_VALUE;
        return Objects.equals(a, b) ? 1d : 0d;
    }

    private static class UserPreference {
        private final Map<String, Double> propertyWeights = new LinkedHashMap<>();
        private final Map<PropertyType, Double> typeWeights = new EnumMap<>(PropertyType.class);
        private double priceSum = 0d;
        private double areaSum = 0d;
        private double totalWeight = 0d;
        private double averagePrice = 0d;
        private double averageArea = 0d;
        private boolean normalized = false;

        void absorb(Property property, double weight) {
            if (property == null || weight <= 0d) {
                return;
            }
            propertyWeights.merge(property.getPropertyId(), weight, Double::sum);
            if (property.getPropertyType() != null) {
                typeWeights.merge(property.getPropertyType(), weight, Double::sum);
            }
            if (property.getPrice() != null) {
                priceSum += property.getPrice().doubleValue() * weight;
            }
            if (property.getArea() != null) {
                areaSum += property.getArea() * weight;
            }
            totalWeight += weight;
        }

        void normalize() {
            if (totalWeight <= 0d || normalized) {
                return;
            }
            propertyWeights.replaceAll((id, weight) -> weight / totalWeight);
            typeWeights.replaceAll((type, weight) -> weight / totalWeight);
            averagePrice = priceSum > 0d ? priceSum / totalWeight : 0d;
            averageArea = areaSum > 0d ? areaSum / totalWeight : 0d;
            normalized = true;
        }

        boolean isEmpty() {
            return propertyWeights.isEmpty();
        }

        Set<String> seedPropertyIds() {
            return propertyWeights.keySet();
        }

        double getPropertyWeight(String propertyId) {
            return propertyWeights.getOrDefault(propertyId, 0d);
        }

        double typeAffinity(Property property) {
            if (property == null || property.getPropertyType() == null) {
                return 0d;
            }
            return typeWeights.getOrDefault(property.getPropertyType(), 0d);
        }

        double priceAffinity(Property property) {
            if (!normalized || averagePrice <= 0d || property == null || property.getPrice() == null) {
                return 0d;
            }
            return similarity(property.getPrice().doubleValue(), averagePrice);
        }

        double areaAffinity(Property property) {
            if (!normalized || averageArea <= 0d || property == null || property.getArea() == null) {
                return 0d;
            }
            return similarity(property.getArea().doubleValue(), averageArea);
        }
    }

    private static class WeightedScore {
        private double totalWeight = 0d;
        private double weightedValue = 0d;
        static final double SKIPPED_VALUE = -1d;

        void add(double weight, double value) {
            totalWeight += weight;
            weightedValue += weight * value;
        }

        void addIfPresent(double weight, double value) {
            if (value == SKIPPED_VALUE) return;
            add(weight, value);
        }

        double finish() {
            if (totalWeight == 0d) return 0d;
            return weightedValue / totalWeight;
        }
    }
}