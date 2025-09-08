package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.fit.entities.Post;
import vn.edu.iuh.fit.entities.enums.PostStatus;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, String> {
    List<Post> findByStatus(PostStatus status);

    @Query("SELECT p FROM Post p WHERE p.status = :status")
    Page<Post> findByStatus(PostStatus status, Pageable pageable);
    Optional<Post> findByPostId(String postId);

    Page<Post> findByLandlord_UserId(String userId, Pageable pageable);
}
