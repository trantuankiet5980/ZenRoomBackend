package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.RefreshToken;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
    RefreshToken findTopByUser_UserIdAndRevokedFalseOrderByExpiresDateDesc(String userId);
    List<RefreshToken> findAllByUser_UserIdAndRevokedFalse(String userId);
}
