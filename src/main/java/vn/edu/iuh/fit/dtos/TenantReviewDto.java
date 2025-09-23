package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.TenantReview}
 */
@Value
public class TenantReviewDto implements Serializable {
    String tenantReviewId;
    BookingDto booking;
    UserDto landlord;
    UserDto tenant;
    Integer rating;
    String comment;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}