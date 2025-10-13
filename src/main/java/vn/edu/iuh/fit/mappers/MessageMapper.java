package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.MessageDto;
import vn.edu.iuh.fit.dtos.PropertyMiniDto;
import vn.edu.iuh.fit.entities.Message;
import vn.edu.iuh.fit.entities.MessageAttachment;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.services.ChatAttachmentService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final ConversationMapper conversationMapper;
    private final UserMapper userMapper;
    private final MessageAttachmentMapper attachmentMapper;
    private final ChatAttachmentService chatAttachmentService;

    public MessageDto toDto(Message entity) {
        if (entity == null) return null;

        return new MessageDto(
                entity.getMessageId(),
                entity.getConversation() != null ? conversationMapper.toDto(entity.getConversation()) : null,
                entity.getSender() != null ? userMapper.toDto(entity.getSender()) : null,
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getIsRead(),
                toMini(entity.getProperty()),
                entity.getAttachments() != null
                        ? entity.getAttachments().stream().map(this::mapAttachment).toList()
                        : List.of()
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
    private PropertyMiniDto toMini(Property p) {
        if (p == null) return null;
        String addr = (p.getAddress() != null) ? p.getAddress().getAddressFull() : null;
        // Nếu có media thumbnail, bạn có thể rút ra ở đây
        String thumb = null; // tuỳ bạn
        return new PropertyMiniDto(
                p.getPropertyId(),
                p.getTitle(),
                p.getPrice(),
                addr,
                thumb
        );
    }
    private vn.edu.iuh.fit.dtos.MessageAttachmentDto mapAttachment(MessageAttachment attachment) {
        var dto = attachmentMapper.toDto(attachment);
        String resolved = chatAttachmentService.resolveUrl(attachment);
        if (dto == null) return null;
        return new vn.edu.iuh.fit.dtos.MessageAttachmentDto(
                dto.getAttachmentId(),
                dto.getMediaType(),
                resolved,
                dto.getContentType(),
                dto.getSize(),
                dto.getCreatedAt()
        );
    }
}
