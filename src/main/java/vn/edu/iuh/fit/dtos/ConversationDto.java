package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Conversation}
 */
@Value
public class ConversationDto implements Serializable {
    String conversationId;
    UserDto tenant;
    UserDto landlord;
    PropertyDto property;
    LocalDateTime createdAt;
}