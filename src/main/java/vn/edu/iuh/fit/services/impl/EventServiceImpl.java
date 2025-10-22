package vn.edu.iuh.fit.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.dtos.RecentlyViewedPropertyDto;
import vn.edu.iuh.fit.dtos.requests.EventRequest;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.UserEvent;
import vn.edu.iuh.fit.entities.enums.EventType;
import vn.edu.iuh.fit.mappers.PropertyMapper;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.UserEventRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.EventService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private final UserEventRepository userEventRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void recordEvent(String userId, EventRequest request) {
        EventType type = request.getEventType();
        if (type == null) {
            throw new IllegalArgumentException("eventType is required");
        }

        if (type == EventType.SEARCH && (request.getQuery() == null || request.getQuery().isBlank())) {
            throw new IllegalArgumentException("query is required for SEARCH events");
        }

        UserEvent.UserEventBuilder builder = UserEvent.builder()
                .eventType(type)
                .occurredAt(LocalDateTime.now())
                .searchQuery(request.getQuery());

        if (userId != null && !userId.isBlank()) {
            userRepository.findById(userId).ifPresent(builder::user);
        }

        if (requiresProperty(type)) {
            String roomId = request.getRoomId();
            if (roomId == null || roomId.isBlank()) {
                throw new IllegalArgumentException("roomId is required for event type " + type);
            }
            Optional<Property> property = propertyRepository.findById(roomId);
            property.ifPresent(builder::property);
        }

        Map<String, Object> metadata = request.getMetadata();
        if (metadata != null && !metadata.isEmpty()) {
            try {
                builder.metadata(objectMapper.writeValueAsString(metadata));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize event metadata", e);
            }
        }

        userEventRepository.save(builder.build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentlyViewedPropertyDto> getRecentlyViewedProperties(String userId, int limit) {
        if (userId == null || userId.isBlank() || limit <= 0) {
            return List.of();
        }

        int pageSize = Math.min(limit, 50);
        Set<EventType> eventTypes = EnumSet.of(EventType.VIEW, EventType.CLICK);

        List<UserEventRepository.RecentPropertyProjection> recent = userEventRepository
                .findRecentProperties(userId, eventTypes, PageRequest.of(0, pageSize));

        if (recent.isEmpty()) {
            return List.of();
        }

        List<String> propertyIds = recent.stream()
                .map(UserEventRepository.RecentPropertyProjection::getPropertyId)
                .collect(Collectors.toList());

        Map<String, PropertyDto> propertyById = propertyRepository.findAllById(propertyIds)
                .stream()
                .map(propertyMapper::toDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PropertyDto::getPropertyId, Function.identity()));

        List<RecentlyViewedPropertyDto> result = new ArrayList<>();
        for (UserEventRepository.RecentPropertyProjection projection : recent) {
            PropertyDto dto = propertyById.get(projection.getPropertyId());
            if (dto != null) {
                result.add(new RecentlyViewedPropertyDto(dto, projection.getLastSeen()));
            }
            if (result.size() == limit) {
                break;
            }
        }

        return result;
    }

    private boolean requiresProperty(EventType type) {
        return switch (type) {
            case VIEW, CLICK, FAVORITE, BOOKING -> true;
            case SEARCH -> false;
        };
    }
}
