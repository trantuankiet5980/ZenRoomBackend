package vn.edu.iuh.fit.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.auths.UserPrincipal;
import vn.edu.iuh.fit.dtos.ApiMessage;
import vn.edu.iuh.fit.dtos.PostDto;
import vn.edu.iuh.fit.dtos.requests.PostRejectRequest;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;
import vn.edu.iuh.fit.entities.Post;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.mappers.PostMapper;
import vn.edu.iuh.fit.repositories.PostRepository;
import vn.edu.iuh.fit.services.PostModerationService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/moderation/posts")
@RequiredArgsConstructor
public class PostModerationController {

    private final PostModerationService postModerationService;
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    @GetMapping("/pending")
    public ResponseEntity<?> listPendingPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> result = postRepository.findByStatus(PostStatus.PENDING, pageable);

        Page<PostDto> dtoPage = result.map(postMapper::toDTO);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(Map.of(
                        "posts", dtoPage.getContent(),
                        "currentPage", dtoPage.getNumber(),
                        "totalItems", dtoPage.getTotalElements(),
                        "totalPages", dtoPage.getTotalPages()
                ))
                .message("Pending posts retrieved successfully")
                .build());
    }

    @PutMapping("/{postId}/approve")
    public ResponseEntity<?> approve(@PathVariable String postId, @AuthenticationPrincipal UserPrincipal actor){
        postModerationService.approve(postId, actor);
        return ResponseEntity.ok(new ApiMessage("Post approved successfully"));
    }

    @PutMapping("/{postId}/reject")
    public ResponseEntity<?> reject(@PathVariable String postId, @Valid @RequestBody PostRejectRequest req, @AuthenticationPrincipal UserPrincipal actor){
        postModerationService.reject(postId, req, actor);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(new ApiMessage("Post rejected successfully"))
                .build());
    }
}
