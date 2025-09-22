package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.ConversationDto;
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

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    // 1) BẮT ĐẦU HỘI THOẠI (nếu đã có thì trả về cái cũ)
    // - currentUser (Principal) là tenant hoặc landlord. Tham số peerId để chỉ người còn lại (nếu không chat theo property).
    // - Nếu có propertyId: xác định landlord từ property, currentUser là tenant.
    @PostMapping("/conversations/start")
    public ConversationDto startConversation(
            Principal principal,
            @RequestParam(required = false) String peerId,
            @RequestParam(required = false) String propertyId
    ) {
        String me = principal.getName();

        User meUser = userRepository.findById(me).orElseThrow();
        User other;
        Property property = null;

        if (propertyId != null) {
            property = propertyRepository.findById(propertyId)
                    .orElseThrow(() -> new IllegalArgumentException("Property not found: " + propertyId));
            other = property.getLandlord();
            if (other == null) throw new IllegalStateException("Property has no landlord");
            if (Objects.equals(other.getUserId(), me))
                throw new IllegalStateException("Landlord cannot start chat with themselves");
            // tìm hội thoại theo (tenant=me, landlord=property.landlord, propertyId)
            var existed = conversationRepository
                    .findByTenant_UserIdAndLandlord_UserIdAndProperty_PropertyId(me, other.getUserId(), propertyId);
            final Property prop = property;
            var conv = existed.orElseGet(() -> {
                Conversation c = new Conversation();
                c.setConversationId(UUID.randomUUID().toString());
                c.setCreatedAt(LocalDateTime.now());
                c.setTenant(meUser);
                c.setLandlord(other);
                c.setProperty(prop);
                return conversationRepository.save(c);
            });
            return conversationMapper.toDto(conv);
        } else {
            // chat 1-1 không gắn property
            if (peerId == null || peerId.isBlank()) {
                throw new IllegalArgumentException("peerId is required when propertyId is null");
            }
            other = userRepository.findById(peerId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + peerId));
            if (Objects.equals(other.getUserId(), me))
                throw new IllegalStateException("Cannot start chat with yourself");

            // Quy ước: me là tenant, other là landlord nếu other là chủ nhà; nếu không, vẫn tạo cặp 2 chiều
            // Để đơn giản: tìm theo (tenant=me AND landlord=other) trước, không có thì đảo chiều.
            Optional<Conversation> existed = conversationRepository
                    .findByTenant_UserIdAndLandlord_UserIdAndProperty_PropertyId(me, other.getUserId(), null);
            Conversation conv = existed.orElseGet(() -> {
                // thử chiều ngược lại
                Optional<Conversation> another = conversationRepository
                        .findByTenant_UserIdAndLandlord_UserIdAndProperty_PropertyId(other.getUserId(), me, null);
                return another.orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setConversationId(UUID.randomUUID().toString());
                    c.setCreatedAt(LocalDateTime.now());
                    c.setTenant(meUser);
                    c.setLandlord(other);
                    c.setProperty(null);
                    return conversationRepository.save(c);
                });
            });
            return conversationMapper.toDto(conv);
        }
    }

    /** Danh sách hội thoại của current user (tenant hoặc landlord) */
    @GetMapping("/conversations")
    public List<ConversationDto> myConversations(Principal principal) {
        String userId = principal.getName();
        var list = conversationRepository
                .findByTenant_UserIdOrLandlord_UserIdOrderByCreatedAtDesc(userId, userId);
        return list.stream().map(conversationMapper::toDto).toList();
    }

    /** Lịch sử tin nhắn phân trang */
    @GetMapping("/{conversationId}/messages")
    public Page<MessageDto> messages(@PathVariable String conversationId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     Principal principal) {
        String userId = principal.getName();
        var c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        boolean isMember = (c.getTenant()!=null && c.getTenant().getUserId().equals(userId))
                || (c.getLandlord()!=null && c.getLandlord().getUserId().equals(userId));
        if (!isMember) throw new SecurityException("Not a member of conversation");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return messageRepository.findByConversation_ConversationId(conversationId, pageable)
                .map(messageMapper::toDto);
    }

    // 4) GỬI TIN NHẮN (HTTP test)
    @PostMapping("/conversations/{conversationId}/messages")
    public MessageDto sendMessage(@PathVariable String conversationId,
                                  @RequestBody SendReq body,
                                  Principal principal) {
        String senderId = principal.getName();

        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        ensureMember(c, senderId);

        User sender = userRepository.findById(senderId).orElseThrow();

        Message m = new Message();
        m.setMessageId(UUID.randomUUID().toString());
        m.setConversation(c);
        m.setSender(sender);
        m.setContent(Optional.ofNullable(body.content()).orElse(""));
        m.setIsRead(false);
        m.setCreatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(m);
        return messageMapper.toDto(saved);
    }

    // 5) ĐÁNH DẤU ĐÃ ĐỌC: theo danh sách messageId
    @PostMapping("/conversations/{conversationId}/read")
    @Transactional
    public int markRead(@PathVariable String conversationId,
                        @RequestBody ReadReq body,
                        Principal principal) {
        String userId = principal.getName();
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        ensureMember(c, userId);

        if (body.messageIds() == null || body.messageIds().isEmpty()) return 0;

        int cnt = 0;
        for (String mid : body.messageIds()) {
            messageRepository.findById(mid).ifPresent(msg -> {
                // chỉ cho phép người nhận đánh dấu đã đọc (không phải chính sender)
                String sender = msg.getSender() != null ? msg.getSender().getUserId() : null;
                if (!Objects.equals(sender, userId) && !Boolean.TRUE.equals(msg.getIsRead())) {
                    msg.setIsRead(true);
                    messageRepository.save(msg);
                }
            });
            cnt++;
        }
        return cnt;
    }

    // 6) ĐÁNH DẤU ĐÃ ĐỌC TẤT CẢ TIN CỦA ĐỐI PHƯƠNG (tuỳ chọn)
    @PostMapping("/conversations/{conversationId}/read-all")
    @Transactional
    public int markAllRead(@PathVariable String conversationId, Principal principal) {
        String userId = principal.getName();
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        ensureMember(c, userId);

        // đọc tất cả tin NHẬN (tức là sender != userId)
        List<Message> unread = messageRepository
                .findByConversation_ConversationIdAndIsReadFalse(conversationId);
        int cnt = 0;
        for (Message m : unread) {
            String sender = m.getSender() != null ? m.getSender().getUserId() : null;
            if (!Objects.equals(sender, userId)) {
                m.setIsRead(true);
                messageRepository.save(m);
                cnt++;
            }
        }
        return cnt;
    }

    // 7) ĐẾM CHƯA ĐỌC THEO HỘI THOẠI
    @GetMapping("/conversations/{conversationId}/unread-count")
    public Map<String, Object> unreadCount(@PathVariable String conversationId, Principal principal) {
        String userId = principal.getName();
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        ensureMember(c, userId);

        int cnt = messageRepository.countByConversation_ConversationIdAndSender_UserIdAndIsReadFalse(
                conversationId, userId
        );
        return Map.of("conversationId", conversationId, "unread", cnt);
    }

    private static void ensureMember(Conversation c, String userId) {
        boolean isMember = (c.getTenant()!=null && c.getTenant().getUserId().equals(userId))
                || (c.getLandlord()!=null && c.getLandlord().getUserId().equals(userId));
        if (!isMember) throw new SecurityException("Not a member of conversation");
    }
    public record SendReq(String content) {}
    public record ReadReq(List<String> messageIds) {}
}
