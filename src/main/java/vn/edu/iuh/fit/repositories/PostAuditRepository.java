package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.PostAudit;

import java.util.List;

public interface PostAuditRepository extends JpaRepository<PostAudit, String> {
    Page<PostAudit> findByPost_PostIdOrderByCreatedAtDesc(String postId, Pageable pageable);
}
