package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import vn.edu.iuh.fit.dtos.MessageDto;
import vn.edu.iuh.fit.entities.Conversation;
import vn.edu.iuh.fit.entities.Message;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.NotificationType;
import vn.edu.iuh.fit.mappers.MessageMapper;
import vn.edu.iuh.fit.repositories.ConversationRepository;
import vn.edu.iuh.fit.repositories.MessageRepository;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.RealtimeNotificationService;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final SimpMessagingTemplate template;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final MessageMapper messageMapper;
    private final RealtimeNotificationService notificationService;

    /** Client gửi tới: /app/chat.start.{propertyId}
     *  Tác dụng: có conversation giữa currentUser và landlord của property, trả về id.
     */
    @MessageMapping("/chat.start.{propertyId}")
    @SendToUser("/queue/chat.conversationId")
    public String startConversation(@DestinationVariable String propertyId, Principal principal) {
        String currentUserId = principal.getName(); // đã map = userId trong CONNECT
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        // Xác định tenant vs landlord: nếu currentUser là landlord của property → không tạo với chính mình
        if (property.getLandlord() != null && property.getLandlord().getUserId().equals(currentUserId)) {
            throw new IllegalStateException("Landlord cannot start chat with themselves");
        }

        String landlordId = property.getLandlord().getUserId();
        String tenantId   = currentUserId;

        Optional<Conversation> existed = conversationRepository
                .findByTenant_UserIdAndLandlord_UserIdAndProperty_PropertyId(tenantId, landlordId, propertyId);

        Conversation c = existed.orElseGet(() -> {
            Conversation nc = new Conversation();
            nc.setConversationId(UUID.randomUUID().toString());
            nc.setCreatedAt(LocalDateTime.now());
            nc.setProperty(property);
            nc.setTenant(userRepository.findById(tenantId).orElseThrow());
            nc.setLandlord(property.getLandlord());
            return conversationRepository.save(nc);
        });

        // FE sẽ nhận id trên kênh /user/queue/chat.conversationId
        return c.getConversationId();
    }

    /** Client gửi tới: /app/chat.send.{conversationId}
     *  Payload: { "content": "Hello" }
     *  Server broadcast: /topic/conversations/{conversationId}
     */
    @MessageMapping("/chat.send.{conversationId}")
    public void sendMessage(@DestinationVariable String conversationId,
                            @Payload MessageSendPayload payload,
                            Principal principal) {
        String senderId = principal.getName();

        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // Chỉ tenant hoặc landlord trong cuộc hội thoại mới được gửi
        boolean isMember = (c.getTenant() != null && c.getTenant().getUserId().equals(senderId))
                || (c.getLandlord() != null && c.getLandlord().getUserId().equals(senderId));
        if (!isMember) throw new SecurityException("Not a member of conversation");

        User sender = userRepository.findById(senderId).orElseThrow();

        Message m = new Message();
        m.setMessageId(UUID.randomUUID().toString());
        m.setConversation(c);
        m.setSender(sender);
        m.setContent(payload.getContent());
        m.setIsRead(false);
        m.setCreatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(m);
        MessageDto dto = messageMapper.toDto(saved);

        // Broadcast tới room topic
        template.convertAndSend("/topic/conversations/" + conversationId, dto);

        // Xác định người nhận để đẩy notify riêng
        String receiverId = c.getTenant().getUserId().equals(senderId)
                ? c.getLandlord().getUserId()
                : c.getTenant().getUserId();

        userRepository.findById(receiverId).ifPresent(receiver -> {
            String title = "Tin nhắn mới";
            String msg   = sender.getFullName() + ": " + payload.getContent();
            String url   = "/app/chat/" + conversationId; // đường dẫn FE của bạn

            notificationService.createAndPush(
                    receiver,
                    title,
                    msg,
                    NotificationType.MESSAGE,
                    url
            );
        });
    }

    /** Đánh dấu đã đọc (client publish tới /app/chat.read.{conversationId} với payload = messageId) */
    @MessageMapping("/chat.read.{conversationId}")
    public void markRead(@DestinationVariable String conversationId,
                         @Payload String messageId,
                         Principal principal) {
        String userId = principal.getName();
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        boolean isMember = (c.getTenant() != null && c.getTenant().getUserId().equals(userId))
                || (c.getLandlord() != null && c.getLandlord().getUserId().equals(userId));
        if (!isMember) throw new SecurityException("Not a member of conversation");

        messageRepository.findById(messageId).ifPresent(msg -> {
            msg.setIsRead(true);
            messageRepository.save(msg);
        });
    }

    // ======= Payload =======
    @lombok.Data
    public static class MessageSendPayload {
        private String content;
    }
}
