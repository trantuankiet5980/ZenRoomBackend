package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.ConversationDto;
import vn.edu.iuh.fit.dtos.ConversationInboxEvent;
import vn.edu.iuh.fit.dtos.MessageDto;
import vn.edu.iuh.fit.entities.Conversation;
import vn.edu.iuh.fit.entities.Message;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.mappers.ConversationMapper;
import vn.edu.iuh.fit.mappers.MessageMapper;
import vn.edu.iuh.fit.repositories.ConversationRepository;
import vn.edu.iuh.fit.repositories.MessageRepository;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.ChatService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final PropertyRepository propertyRepo;
    private final UserRepository userRepo;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final SimpMessagingTemplate messaging;

    @Transactional
    @Override
    public MessageDto send(String currentUserId, SendCommand cmd) {
        if ((cmd.conversationId()==null || cmd.conversationId().isBlank())
                && (cmd.peerId()==null || cmd.peerId().isBlank())
                && (cmd.propertyId()==null || cmd.propertyId().isBlank())) {
            throw new IllegalArgumentException("Provide conversationId or (peerId/propertyId)");
        }
        if (cmd.content()==null || cmd.content().isBlank())
            throw new IllegalArgumentException("content is required");

        Conversation conv;
        User me = userRepo.findById(currentUserId).orElseThrow();

        if (cmd.conversationId()!=null && !cmd.conversationId().isBlank()) {
            conv = conversationRepo.findById(cmd.conversationId()).orElseThrow();
            ensureMember(conv, currentUserId);
        } else {
            // Tự tạo / lấy conversation theo peerId + propertyId
            User other;
            Property prop = null;

            if (cmd.propertyId()!=null && !cmd.propertyId().isBlank()) {
                prop = propertyRepo.findById(cmd.propertyId())
                        .orElseThrow(() -> new IllegalArgumentException("Property not found"));
                other = prop.getLandlord();
                if (other==null) throw new IllegalStateException("Property has no landlord");
                if (Objects.equals(other.getUserId(), currentUserId))
                    throw new IllegalStateException("Landlord cannot chat with themselves");
                // current user = tenant, other = landlord
                Property finalProp = prop;
                conv = conversationRepo
                        .findByTenant_UserIdAndLandlord_UserIdAndProperty_PropertyId(
                                currentUserId, other.getUserId(), prop.getPropertyId()
                        ).orElseGet(() -> {
                            Conversation c = new Conversation();
                            c.setConversationId(UUID.randomUUID().toString());
                            c.setCreatedAt(LocalDateTime.now());
                            c.setTenant(me);
                            c.setLandlord(other);
                            c.setProperty(finalProp);
                            return conversationRepo.save(c);
                        });
            } else {
                // 1-1 không gắn property
                if (cmd.peerId()==null || cmd.peerId().isBlank())
                    throw new IllegalArgumentException("peerId is required if propertyId is null");
                other = userRepo.findById(cmd.peerId())
                        .orElseThrow(() -> new IllegalArgumentException("Peer not found"));
                if (Objects.equals(other.getUserId(), currentUserId))
                    throw new IllegalStateException("Cannot chat with yourself");

                Optional<Conversation> existed = conversationRepo
                        .findByTenant_UserIdAndLandlord_UserIdAndProperty_PropertyId(currentUserId, other.getUserId(), null);
                conv = existed.orElseGet(() -> {
                    Optional<Conversation> reverse = conversationRepo
                            .findByTenant_UserIdAndLandlord_UserIdAndProperty_PropertyId(other.getUserId(), currentUserId, null);
                    return reverse.orElseGet(() -> {
                        Conversation c = new Conversation();
                        c.setConversationId(UUID.randomUUID().toString());
                        c.setCreatedAt(LocalDateTime.now());
                        c.setTenant(me);
                        c.setLandlord(other);
                        c.setProperty(null);
                        return conversationRepo.save(c);
                    });
                });
            }
        }

        Message m = new Message();
        m.setMessageId(UUID.randomUUID().toString());
        m.setConversation(conv);
        m.setSender(me);
        m.setContent(cmd.content());
        m.setIsRead(false);
        m.setCreatedAt(LocalDateTime.now());
        Message saved = messageRepo.save(m);

        MessageDto dto = messageMapper.toDto(saved);
        messaging.convertAndSend(topic(conv.getConversationId()), dto);
        emitInboxForBoth(conv);

        return dto;
    }

        @Override
    public Page<MessageDto> getMessages(String currentUserId, String conversationId, Pageable pageable) {
        Conversation c = conversationRepo.findById(conversationId).orElseThrow();
        ensureMember(c, currentUserId);
        return messageRepo.findByConversation_ConversationId(conversationId, pageable)
                .map(messageMapper::toDto);
    }

    @Override
    public List<ConversationDto> myConversations(String currentUserId) {
        return conversationRepo.findByTenant_UserIdOrLandlord_UserIdOrderByCreatedAtDesc(currentUserId, currentUserId)
                .stream().map(conversationMapper::toDto).toList();
    }

    @Transactional
    @Override
    public int markRead(String currentUserId, String conversationId, List<String> messageIds) {
        Conversation c = conversationRepo.findById(conversationId).orElseThrow();
        ensureMember(c, currentUserId);
        if (messageIds==null || messageIds.isEmpty()) return 0;

        int cnt = 0;
        for (String mid : messageIds) {
            messageRepo.findById(mid).ifPresent(msg -> {
                String senderId = (msg.getSender()!=null)? msg.getSender().getUserId() : null;
                if (!Objects.equals(senderId, currentUserId) && !Boolean.TRUE.equals(msg.getIsRead())) {
                    msg.setIsRead(true);
                    messageRepo.save(msg);
                    messaging.convertAndSend(topicRead(conversationId), mid);
                }
            });
            cnt++;
        }
        return cnt;
    }

    @Transactional
    @Override
    public int markAllRead(String currentUserId, String conversationId) {
        Conversation c = conversationRepo.findById(conversationId).orElseThrow();
        ensureMember(c, currentUserId);
        var unread = messageRepo.findByConversation_ConversationIdAndIsReadFalse(conversationId);
        int cnt = 0;
        for (Message m : unread) {
            String senderId = (m.getSender()!=null)? m.getSender().getUserId() : null;
            if (!Objects.equals(senderId, currentUserId)) {
                m.setIsRead(true);
                messageRepo.save(m);
                cnt++;
            }
        }
        if (cnt>0) {
            messaging.convertAndSend(topicReadAll(conversationId), currentUserId);
            emitInboxForBoth(c);
        }
        return cnt;
    }

    @Override
    public int unreadCount(String currentUserId, String conversationId) {
        Conversation c = conversationRepo.findById(conversationId).orElseThrow();
        ensureMember(c, currentUserId);
        return messageRepo.countUnreadForReceiver(conversationId, currentUserId);
    }

    @Override
    public MessageDto lastMessage(String currentUserId, String conversationId) {
        Conversation c = conversationRepo.findById(conversationId).orElseThrow();
        ensureMember(c, currentUserId);
        var m = messageRepo.findFirstByConversation_ConversationIdOrderByCreatedAtDesc(conversationId);
        return (m==null)? null : messageMapper.toDto(m);
    }

    private void ensureMember(Conversation c, String userId) {
        boolean isMember = (c.getTenant()!=null && c.getTenant().getUserId().equals(userId))
                || (c.getLandlord()!=null && c.getLandlord().getUserId().equals(userId));
        if (!isMember) throw new SecurityException("Not a member of conversation");
    }

    private String topic(String conversationId) { return "/topic/chat." + conversationId; }
    private String inboxQ()                         { return "/queue/chat.inbox"; }
    // === Helper: phát inbox cho 1 user ===
    private void emitInboxForUser(Conversation conv, String userId) {
        var last = messageRepo.findFirstByConversation_ConversationIdOrderByCreatedAtDesc(
                conv.getConversationId());

        int unread = messageRepo.countUnreadForReceiver(conv.getConversationId(), userId);

        var evt = new ConversationInboxEvent(
                conv.getConversationId(),
                (last != null ? messageMapper.toDto(last) : null),
                unread,
                (last != null ? last.getCreatedAt() : conv.getCreatedAt())
        );

        // /user/{userId}/queue/chat.inbox
        messaging.convertAndSendToUser(userId, inboxQ(), evt);
    }

    // === Helper: phát inbox cho 2 phía ===
    private void emitInboxForBoth(Conversation conv) {
        if (conv.getTenant() != null)
            emitInboxForUser(conv, conv.getTenant().getUserId());
        if (conv.getLandlord() != null)
            emitInboxForUser(conv, conv.getLandlord().getUserId());
    }
    private String topicRead(String conversationId) { return "/topic/chat." + conversationId + ".read"; }
    private String topicReadAll(String conversationId) { return "/topic/chat." + conversationId + ".read-all"; }
}
