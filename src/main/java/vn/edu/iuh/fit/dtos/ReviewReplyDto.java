package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.ReviewReply}
 */
@Value
public class ReviewReplyDto implements Serializable {
    String replyId;
    ReviewDto review;
    UserDto landlord;
    String replyText;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}