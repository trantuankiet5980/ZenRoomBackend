package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.UserDto;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.UserFollow;
import vn.edu.iuh.fit.mappers.UserMapper;
import vn.edu.iuh.fit.repositories.UserFollowRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.AuthService;
import vn.edu.iuh.fit.services.FollowService;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final AuthService authService;
    private final UserRepository userRepo;
    private final UserFollowRepository followRepo;
    private final UserMapper userMapper;

    @Transactional
    @Override
    public void follow(String targetUserId) {
        String me = authService.getCurrentUserId();
        if (me.equals(targetUserId)) throw new IllegalArgumentException("Không thể tự theo dõi chính mình");
        if (followRepo.existsByFollower_UserIdAndFollowing_UserId(me, targetUserId)) return;

        User follower = userRepo.getReferenceById(me);
        User following = userRepo.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + targetUserId));

        followRepo.save(UserFollow.builder().follower(follower).following(following).build());
    }

    @Transactional
    @Override
    public void unfollow(String targetUserId) {
        String me = authService.getCurrentUserId();
        followRepo.deleteByFollower_UserIdAndFollowing_UserId(me, targetUserId);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserDto> listFollowers(String userId, Pageable pageable) {
        return followRepo.findByFollowing_UserId(userId, pageable)
                .map(f -> userMapper.toDto(f.getFollower()));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserDto> listFollowing(String userId, Pageable pageable) {
        return followRepo.findByFollowing_UserId(userId, pageable)
                .map(f -> userMapper.toDto(f.getFollower()));
    }

    @Override
    public long countFollowers(String userId) {
        return followRepo.countByFollowing_UserId(userId);
    }

    @Override
    public long countFollowing(String userId) {
        return followRepo.countByFollower_UserId(userId);
    }
}
