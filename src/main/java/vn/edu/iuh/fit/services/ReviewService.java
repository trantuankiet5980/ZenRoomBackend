package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.iuh.fit.dtos.ReviewDto;
import vn.edu.iuh.fit.dtos.ReviewReplyDto;
import vn.edu.iuh.fit.dtos.ReviewStatsDto;

public interface ReviewService {
    // Tenant upsert (reviewId == null -> create; != null -> update trong 24h)
    ReviewDto upsertTenantReview(String tenantId, ReviewDto dto);

    // Tenant xóa trong 24h
    void deleteTenantReview(String tenantId, String reviewId);

    // Landlord upsert reply (replyId == null -> create; != null -> update trong 24h)
    ReviewReplyDto upsertLandlordReply(String landlordId, ReviewReplyDto dto);

    ReviewDto getByBooking(String bookingId, String requesterId);
    Page<ReviewDto> listByProperty(String propertyId, Pageable pageable);
    Page<ReviewDto> listByTenant(String tenantId, Pageable pageable);

    ReviewStatsDto getLandlordReviewStats(String landlordId);
}
