package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.auths.UserPrincipal;
import vn.edu.iuh.fit.dtos.requests.PostRejectRequest;
import vn.edu.iuh.fit.entities.Post;
import vn.edu.iuh.fit.entities.PostAudit;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.PostAuditAction;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.repositories.PostAuditRepository;
import vn.edu.iuh.fit.repositories.PostRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.PostModerationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostModerationServiceImpl implements PostModerationService {

    private final PostRepository postRepository;
    private final PostAuditRepository postAuditRepository;
    private final UserRepository userRepository;

    private Post getPendingOrThrow(String postId){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));
        if (post.getStatus() != PostStatus.PENDING) {
            throw new IllegalStateException("Post with ID: " + postId + " is not in PENDING status");
        }
        return post;
    }

    private void log(Post post, PostAuditAction action, PostStatus from, PostStatus to, String reason, User actor) {
        PostAudit audit = PostAudit.builder()
                .post(post).action(action).fromStatus(from).toStatus(to)
                .reason(reason).actor(actor).build();
        postAuditRepository.save(audit);
    }

    @Override
    public void approve(String postId, UserPrincipal actor) {
        Post post = getPendingOrThrow(postId);
        PostStatus from = post.getStatus();

        post.setStatus(PostStatus.APPROVED);
        post.setPublishedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.setRejectedReason(null);
        postRepository.save(post);

        User actorEntity = (actor != null && actor.getUserResponse() != null)
                ? userRepository.findById(actor.getUserResponse().getUserId()).orElse(null)
                : null;

        log(post, PostAuditAction.APPROVE, from, post.getStatus(), null, actorEntity);
    }

    @Override
    public void reject(String postId, PostRejectRequest req, UserPrincipal actor) {
        Post post = getPendingOrThrow(postId);
        if (req == null || req.getReason() == null || req.getReason().isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        PostStatus from = post.getStatus();
        post.setStatus(PostStatus.REJECTED);
        post.setPublishedAt(null);
        post.setRejectedReason(req.getReason());
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);

        User actorEntity = (actor != null && actor.getUserResponse() != null)
                ? userRepository.findById(actor.getUserResponse().getUserId()).orElse(null)
                : null;

        log(post, PostAuditAction.REJECT, from, post.getStatus(), req.getReason(), actorEntity);
    }
}
