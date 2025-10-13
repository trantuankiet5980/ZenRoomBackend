package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.iuh.fit.dtos.ConversationDto;
import vn.edu.iuh.fit.dtos.MessageDto;
import vn.edu.iuh.fit.entities.enums.MediaType;

import java.util.List;

public interface ChatService {
    MessageDto send(String currentUserId, SendCommand cmd);
    Page<MessageDto> getMessages(String currentUserId, String conversationId, Pageable pageable);
    List<ConversationDto> myConversations(String currentUserId);
    int markRead(String currentUserId, String conversationId, List<String> messageIds);
    int markAllRead(String currentUserId, String conversationId);
    int unreadCount(String currentUserId, String conversationId);
    MessageDto lastMessage(String currentUserId, String conversationId);

    record AttachmentPayload(String storageKey, String url, MediaType mediaType, String contentType, Long size) {}

    record SendCommand(String conversationId, String propertyId, String peerId, String content,
                       List<AttachmentPayload> attachments) {}
}
