package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.iuh.fit.dtos.UserDto;

public interface FollowService {
    void follow(String targetUserId);   // currentUser -> follow target
    void unfollow(String targetUserId);
    Page<UserDto> listFollowers(String userId, Pageable pageable);
    Page<UserDto> listFollowing(String userId, Pageable pageable);
    long countFollowers(String userId);
    long countFollowing(String userId);
}
