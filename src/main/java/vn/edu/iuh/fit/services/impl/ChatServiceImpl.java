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
import vn.edu.iuh.fit.entities.MessageAttachment;
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
import java.util.*;

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
            throw new IllegalArgumentException("Provide conversationId or peerId/propertyId");
        }
        List<ChatService.AttachmentPayload> attachments = (cmd.attachments() != null)
                ? cmd.attachments().stream().filter(Objects::nonNull).toList()
                : List.of();
        if (attachments.size() > 10) {
            throw new IllegalArgumentException("Maximum 10 attachments per message");
        }

        boolean hasContent = cmd.content() != null && !cmd.content().isBlank();
        if (!hasContent && attachments.isEmpty())
            throw new IllegalArgumentException("Message content or images are required");

        User me = userRepo.findById(currentUserId).orElseThrow();

        Conversation conv;
        if (cmd.conversationId()!=null && !cmd.conversationId().isBlank()) {
            conv = conversationRepo.findById(cmd.conversationId()).orElseThrow();
            ensureMember(conv, currentUserId);
        } else {
            // Xác định landlord và tenant
            User landlord;
            User tenant;

            if (cmd.propertyId()!=null && !cmd.propertyId().isBlank()) {
                Property prop = propertyRepo.findById(cmd.propertyId())
                        .orElseThrow(() -> new IllegalArgumentException("Property not found"));
                landlord = prop.getLandlord();
                if (landlord == null) throw new IllegalStateException("Property has no landlord");
                if (Objects.equals(landlord.getUserId(), me.getUserId()))
                    throw new IllegalStateException("Landlord cannot chat with themselves");
                tenant = me;
            } else {
                User peer = userRepo.findById(cmd.peerId())
                        .orElseThrow(() -> new IllegalArgumentException("Peer not found"));
                if (Objects.equals(peer.getUserId(), me.getUserId()))
                    throw new IllegalStateException("Cannot chat with yourself");

                Optional<Conversation> existed = conversationRepo.findByUsersAnyOrder(me.getUserId(), peer.getUserId());
                if (existed.isPresent()) {
                    conv = existed.get();
                } else {
                    tenant = me;
                    landlord = peer;
                    conv = createConversation(tenant, landlord);
                }

                Message saved = persistMessage(conv, me, cmd.content(), null /* no property */, attachments);
                notify(conv, saved);
                return messageMapper.toDto(saved);
            }

            final String tenantId = tenant.getUserId();
            final String landlordId = landlord.getUserId();

            conv = conversationRepo.findByTenant_UserIdAndLandlord_UserId(tenantId, landlordId)
                    .orElseGet(() -> createConversation(tenant, landlord));

            Property prop = propertyRepo.getReferenceById(cmd.propertyId());

            Message saved = persistMessage(conv, me, cmd.content(), prop, attachments);
            notify(conv, saved);
            return messageMapper.toDto(saved);
        }

        Property prop = (cmd.propertyId()!=null && !cmd.propertyId().isBlank())
                ? propertyRepo.findById(cmd.propertyId()).orElse(null)
                : null;

        Message saved = persistMessage(conv, me, cmd.content(), prop, attachments);
        notify(conv, saved);
        return messageMapper.toDto(saved);
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

    private Conversation createConversation(User tenant, User landlord) {
        Conversation c = new Conversation();
        c.setConversationId(UUID.randomUUID().toString());
        c.setCreatedAt(LocalDateTime.now());
        c.setTenant(tenant);
        c.setLandlord(landlord);
        // KHÔNG đặt property ở đây nữa
        return conversationRepo.save(c);
    }

    private Message persistMessage(Conversation conv, User sender, String content, Property prop,
                                   List<ChatService.AttachmentPayload> attachments) {
        Message m = new Message();
        m.setMessageId(UUID.randomUUID().toString());
        m.setConversation(conv);
        m.setSender(sender);
        m.setProperty(prop);
        m.setContent(content);
        m.setIsRead(false);
        m.setCreatedAt(LocalDateTime.now());
        if (attachments != null && !attachments.isEmpty()) {
            List<MessageAttachment> attEntities = new ArrayList<>();
            for (ChatService.AttachmentPayload payload : attachments) {
                if (payload == null) continue;
                MessageAttachment attachment = MessageAttachment.builder()
                        .attachmentId(UUID.randomUUID().toString())
                        .message(m)
                        .mediaType(payload.mediaType())
                        .storageKey(payload.storageKey())
                        .url(payload.url())
                        .contentType(payload.contentType())
                        .size(payload.size())
                        .createdAt(LocalDateTime.now())
                        .build();
                attEntities.add(attachment);
            }
            m.setAttachments(attEntities);
        }
        return messageRepo.save(m);
    }
    private String topicNotify(String userId) {
        return "/topic/notify." + userId;
    }
    private void notify(Conversation conv, Message saved) {
        var dto = messageMapper.toDto(saved);
        messaging.convertAndSend("/topic/chat." + conv.getConversationId(), dto);
        emitInboxForBoth(conv); // giữ logic unread/last

        // Xác định ai là người nhận (không phải người gửi)
        String senderId = saved.getSender() != null ? saved.getSender().getUserId() : null;

        if (conv.getTenant() != null && !conv.getTenant().getUserId().equals(senderId)) {
            String uid = conv.getTenant().getUserId();
            System.out.println("[WS][NOTIFY_TOPIC] to tenant=" + uid);
            // gửi theo topic riêng
            messaging.convertAndSend(topicNotify(uid), "Bạn có tin nhắn mới");
            // (tuỳ chọn) vẫn gửi user-queue như cũ:
            messaging.convertAndSendToUser(uid, "/queue/notify", "Bạn có tin nhắn mới");
        }
        if (conv.getLandlord() != null && !conv.getLandlord().getUserId().equals(senderId)) {
            String uid = conv.getLandlord().getUserId();
            System.out.println("[WS][NOTIFY_TOPIC] to landlord=" + uid);
            messaging.convertAndSend(topicNotify(uid), "Bạn có tin nhắn mới");
            messaging.convertAndSendToUser(uid, "/queue/notify", "Bạn có tin nhắn mới");
        }
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
