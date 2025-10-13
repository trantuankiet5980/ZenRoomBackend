package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.MessageAttachmentDto;
import vn.edu.iuh.fit.entities.MessageAttachment;

@Component
public class MessageAttachmentMapper {
    public MessageAttachmentDto toDto(MessageAttachment entity) {
        if (entity == null) {
            return null;
        }
        return new MessageAttachmentDto(
                entity.getAttachmentId(),
                entity.getMediaType(),
                entity.getUrl(),
                entity.getContentType(),
                entity.getSize(),
                entity.getCreatedAt()
        );
    }
}
