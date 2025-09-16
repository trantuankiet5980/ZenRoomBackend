package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entities.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<User> findByUserId(String userId);

    boolean existsByPhoneNumberAndUserIdNot(String phoneNumber, String userId);
    boolean existsByEmailAndUserIdNot(String email, String userId);

    @Query("select u from User u where u.status = vn.edu.iuh.fit.entities.enums.UserStatus.DELETED " +
            "and u.deleteEffectiveAt <= :cutoff")
    List<User> findAllEligibleForHardDelete(LocalDateTime cutoff);

    List<User> findByRole_RoleName(String roleName);
}
