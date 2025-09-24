package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class ConversationInboxEvent {
    String conversationId;
    MessageDto lastMessage;   // để FE hiển thị snippet + time
    int unread;               // số chưa đọc của người NHẬN event
    LocalDateTime updatedAt;  // thuận tiện sort client
}
