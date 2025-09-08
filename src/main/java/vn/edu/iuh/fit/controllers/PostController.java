package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.PostDto;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;
import vn.edu.iuh.fit.entities.Post;
import vn.edu.iuh.fit.mappers.PostMapper;
import vn.edu.iuh.fit.repositories.PostRepository;
import vn.edu.iuh.fit.services.PostService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PostDto dto) {
        try {
            Post p = postService.create(dto);
            PostDto postDto = postMapper.toDTO(p);
            if (p == null) {
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("Post creation failed")
                        .data(null)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Post created successfully")
                    .data(postDto)
                    .build());
//            return ResponseEntity.status(HttpStatus.CREATED).body(p.getPostId());
        } catch (IllegalArgumentException | jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    ApiResponse.builder()
                            .success(false)
                            .message("An unexpected error occurred: " + e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getListPostByUserId(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            ){
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> result = postRepository.findByLandlord_UserId(userId, pageable);

        Page<PostDto> dtoPage = result.map(postMapper::toDTO);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(Map.of(
                        "totalElements", dtoPage.getTotalElements(),
                        "totalPages", dtoPage.getTotalPages(),
                        "page", dtoPage.getNumber(),
                        "size", dtoPage.getSize(),
                        "content", dtoPage.getContent()
                ))
                .message("List of posts for user " + userId)
                .build());
    }

}
