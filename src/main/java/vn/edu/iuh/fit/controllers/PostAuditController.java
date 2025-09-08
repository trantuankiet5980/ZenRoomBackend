package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.PostAuditDto;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;
import vn.edu.iuh.fit.entities.PostAudit;
import vn.edu.iuh.fit.mappers.PostAuditMapper;
import vn.edu.iuh.fit.repositories.PostAuditRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit/posts")
@RequiredArgsConstructor
public class PostAuditController {

    private final PostAuditRepository postAuditRepository;
    private final PostAuditMapper postAuditMapper;

    @GetMapping("/{postId}")
    public ResponseEntity<?> listAudits(@PathVariable String postId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostAudit> result = postAuditRepository.findByPost_PostIdOrderByCreatedAtDesc(postId, pageable);

        Page<PostAuditDto> dtoPage = result.map(postAuditMapper::toDto);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(Map.of(
                        "totalElements", dtoPage.getTotalElements(),
                        "totalPages", dtoPage.getTotalPages(),
                        "page", dtoPage.getNumber(),
                        "size", dtoPage.getSize(),
                        "content", dtoPage.getContent()
                ))
                .message("List of audits retrieved successfully")
                .build());
    }
}
