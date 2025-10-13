package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.fit.entities.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {
    @EntityGraph(attributePaths = {"conversation","sender","property","property.address","attachments"})
    Page<Message> findByConversation_ConversationId(String conversationId, Pageable pageable);
    List<Message> findByConversation_ConversationIdAndIsReadFalse(String conversationId);
    //get last message in conversation
    @EntityGraph(attributePaths = {"conversation","sender","property","property.address","attachments"})
    Message findFirstByConversation_ConversationIdOrderByCreatedAtDesc(String conversationId);

    // Đếm tin NHẬN chưa đọc (sender != currentUser)
    @Query("""
        select count(m) from Message m
        where m.conversation.conversationId = :conversationId
          and m.isRead = false
          and m.sender.userId <> :currentUserId
    """)
    int countUnreadForReceiver(String conversationId, String currentUserId);

}
