package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.Conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    Optional<Conversation> findByTenant_UserIdAndLandlord_UserIdAndProperty_PropertyId(
            String tenantId, String landlordId, String propertyId);

    List<Conversation> findByTenant_UserIdOrLandlord_UserIdOrderByCreatedAtDesc(
            String tenantId, String landlordId);
}
