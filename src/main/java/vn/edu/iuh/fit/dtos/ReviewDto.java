package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Review}
 */
@Value
public class ReviewDto implements Serializable {
    String reviewId;
    BookingDto booking;
    UserDto tenant;
    int rating;
    String comment;
    ReviewReplyDto reply;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}