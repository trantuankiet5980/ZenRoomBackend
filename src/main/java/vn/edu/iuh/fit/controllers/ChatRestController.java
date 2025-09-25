package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.ConversationDto;
import vn.edu.iuh.fit.dtos.MessageDto;
import vn.edu.iuh.fit.services.ChatService;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chat;

    // GỬI TIN (tự tạo conversation nếu chưa có)
    @PostMapping("/send")
    public MessageDto send(@RequestBody SendReq body, Principal principal) {
        var cmd = new ChatService.SendCommand(
                body.conversationId(), body.propertyId(), body.peerId(), body.content()
        );
        return chat.send(principal.getName(), cmd);
    }

    // Danh sách hội thoại của tôi
    @GetMapping("/conversations")
    public List<ConversationDto> myConversations(Principal principal) {
        return chat.myConversations(principal.getName());
    }

    // Lịch sử tin nhắn (phân trang)
    @GetMapping("/{conversationId}/messages")
    public Page<MessageDto> messages(@PathVariable String conversationId,
                                     @RequestParam(defaultValue="0") int page,
                                     @RequestParam(defaultValue="20") int size,
                                     @RequestParam(defaultValue="createdAt,ASC") String sort,
                                     Principal principal) {
        Sort s = parseSort(sort);
        Pageable p = PageRequest.of(page, size, s);
        return chat.getMessages(principal.getName(), conversationId, p);
    }

    // Đọc một số tin
    @PostMapping("/{conversationId}/read")
    public int markRead(@PathVariable String conversationId, @RequestBody ReadReq body, Principal principal) {
        return chat.markRead(principal.getName(), conversationId, body.messageIds());
    }

    // Đọc hết
    @PostMapping("/{conversationId}/read-all")
    public int markAllRead(@PathVariable String conversationId, Principal principal) {
        return chat.markAllRead(principal.getName(), conversationId);
    }

    // Đếm chưa đọc
    @GetMapping("/{conversationId}/unread-count")
    public Map<String, Object> unread(@PathVariable String conversationId, Principal principal) {
        int cnt = chat.unreadCount(principal.getName(), conversationId);
        return Map.of("conversationId", conversationId, "unread", cnt);
    }

    // Tin gần nhất
    @GetMapping("/{conversationId}/last-message")
    public MessageDto last(@PathVariable String conversationId, Principal principal) {
        return chat.lastMessage(principal.getName(), conversationId);
    }

    private Sort parseSort(String sort) {
        String[] p = sort.split(",");
        if (p.length==2) return Sort.by("DESC".equalsIgnoreCase(p[1])? Sort.Direction.DESC: Sort.Direction.ASC, p[0]);
        return Sort.by(Sort.Direction.ASC, "createdAt");
    }

    public record SendReq(String conversationId, String propertyId, String peerId, String content) {}
    public record ReadReq(List<String> messageIds) {}
}
