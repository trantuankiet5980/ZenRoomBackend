package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.MessageDto;
import vn.edu.iuh.fit.entities.Message;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final ConversationMapper conversationMapper;
    private final UserMapper userMapper;

    public MessageDto toDto(Message entity) {
        if (entity == null) return null;

        return new MessageDto(
                entity.getMessageId(),
                entity.getConversation() != null ? conversationMapper.toDto(entity.getConversation()) : null,
                entity.getSender() != null ? userMapper.toDto(entity.getSender()) : null,
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getIsRead()
        );
    }

    public Message toEntity(MessageDto dto) {
        if (dto == null) return null;

        Message entity = new Message();
        entity.setMessageId(dto.getMessageId());
        entity.setContent(dto.getContent());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setIsRead(dto.getIsRead());

        // Conversation & Sender sẽ được gán trong Service bằng cách fetch từ DB theo id
        return entity;
    }
}
