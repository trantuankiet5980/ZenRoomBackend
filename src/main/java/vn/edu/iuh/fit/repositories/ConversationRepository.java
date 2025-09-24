package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.fit.entities.Conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {

    // Tìm đúng chiều (tenant, landlord)
    Optional<Conversation> findByTenant_UserIdAndLandlord_UserId(String tenantId, String landlordId);

    // Tìm bất kỳ chiều (phòng trường hợp gọi 1-1 mà không biết ai là tenant)
    @Query("""
     select c from Conversation c
     where (c.tenant.userId = :u1 and c.landlord.userId = :u2)
        or (c.tenant.userId = :u2 and c.landlord.userId = :u1)
  """)
    Optional<Conversation> findByUsersAnyOrder(String u1, String u2);

    List<Conversation> findByTenant_UserIdOrLandlord_UserIdOrderByCreatedAtDesc(String t, String l);
}
