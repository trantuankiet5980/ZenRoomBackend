package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.iuh.fit.dtos.TenantReviewDto;
import vn.edu.iuh.fit.dtos.requests.TenantReviewCreateRequest;
import vn.edu.iuh.fit.dtos.requests.TenantReviewUpdateRequest;

public interface TenantReviewService {
    TenantReviewDto upsert(String landlordId, TenantReviewDto dto); // insert/update (24h)
    void delete(String landlordId, String tenantReviewId);          // delete (24h)

    TenantReviewDto getByBooking(String bookingId, String requesterId);
    Page<TenantReviewDto> listAboutTenant(String tenantId, Pageable pageable);
    Page<TenantReviewDto> listByLandlord(String landlordId, Pageable pageable);
    Page<String> listPendingBookingsToRate(String landlordId, Pageable pageable);
}
