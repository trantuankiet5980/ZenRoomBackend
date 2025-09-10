package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.UserFollow;

public interface UserFollowRepository extends JpaRepository<UserFollow, String> {
    boolean existsByFollower_UserIdAndFollowing_UserId(String followerId, String followingId);
    void deleteByFollower_UserIdAndFollowing_UserId(String followerId, String followingId);

    Page<UserFollow> findByFollower_UserId(String followerId, Pageable pageable);   // danh sách đang theo dõi
    Page<UserFollow> findByFollowing_UserId(String followingId, Pageable pageable); // danh sách người theo dõi

    long countByFollower_UserId(String followerId);   // following count
    long countByFollowing_UserId(String followingId); // followers count
}
