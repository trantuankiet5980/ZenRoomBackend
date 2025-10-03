package vn.edu.iuh.fit.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.requests.EventRequest;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.UserEvent;
import vn.edu.iuh.fit.entities.enums.EventType;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.UserEventRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.EventService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private final UserEventRepository userEventRepository;
    private UserRepository userRepository;
    private final PropertyRepository propertyRepository;
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

    private boolean requiresProperty(EventType type) {
        return switch (type) {
            case VIEW, CLICK, FAVORITE, BOOKING -> true;
            case SEARCH -> false;
        };
    }
}
