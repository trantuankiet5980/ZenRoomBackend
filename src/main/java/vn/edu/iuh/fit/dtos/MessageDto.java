package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Message}
 */
@Value
public class MessageDto implements Serializable {
    String messageId;
    ConversationDto conversation;
    UserDto sender;
    String content;
    LocalDateTime createdAt;
    Boolean isRead;

    PropertyMiniDto property;
    List<MessageAttachmentDto> attachments;
}