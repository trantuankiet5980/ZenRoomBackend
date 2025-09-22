package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {
    Page<Message> findByConversation_ConversationId(String conversationId, Pageable pageable);
    List<Message> findByConversation_ConversationIdAndIsReadFalse(String conversationId);
    int countByConversation_ConversationIdAndSender_UserIdAndIsReadFalse(String conversationId, String notSenderId);
    //get last message in conversation
    Message findFirstByConversation_ConversationIdOrderByCreatedAtDesc(String conversationId);
}
