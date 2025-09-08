package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.repositories.PostAuditRepository;

@RestController
@RequestMapping("/api/v1/audit/posts")
@RequiredArgsConstructor
public class PostAuditController {

    private final PostAuditRepository postAuditRepository;

    @GetMapping("/{postId}/audits")
    public ResponseEntity<?> listAudits(@PathVariable String postId){
        return ResponseEntity.ok(postAuditRepository.findByPost_PostIdOrderByCreatedAt(postId));
    }
}
