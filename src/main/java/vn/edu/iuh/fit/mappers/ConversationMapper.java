package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.ConversationDto;
import vn.edu.iuh.fit.entities.Conversation;

@Component
@RequiredArgsConstructor
public class ConversationMapper {

    private final UserMapper userMapper;
    private final PropertyMapper propertyMapper;

    public ConversationDto toDto(Conversation entity) {
        if (entity == null) return null;

        return new ConversationDto(
                entity.getConversationId(),
                entity.getTenant() != null ? userMapper.toDto(entity.getTenant()) : null,
                entity.getLandlord() != null ? userMapper.toDto(entity.getLandlord()) : null,
                entity.getProperty() != null ? propertyMapper.toDto(entity.getProperty()) : null,
                entity.getCreatedAt()
        );
    }

    public Conversation toEntity(ConversationDto dto) {
        if (dto == null) return null;

        Conversation entity = new Conversation();
        entity.setConversationId(dto.getConversationId());
        entity.setCreatedAt(dto.getCreatedAt());

        // tenant, landlord, property sẽ được gán trong Service
        // bằng cách fetch từ DB theo id, tránh vòng lặp
        return entity;
    }
}
