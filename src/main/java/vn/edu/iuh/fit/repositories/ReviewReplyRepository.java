package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.ReviewReply;

import java.util.Optional;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, String> {
    Optional<ReviewReply> findByReview_ReviewId(String reviewId);
    boolean existsByReview_ReviewId(String reviewId); // giới hạn 1 reply/review
}
