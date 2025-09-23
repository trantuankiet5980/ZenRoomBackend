package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.fit.entities.TenantReview;

import java.util.List;
import java.util.Optional;

public interface TenantReviewRepository extends JpaRepository<TenantReview, String> {
    Optional<TenantReview> findByBooking_BookingId(String bookingId);
    boolean existsByBooking_BookingId(String bookingId); // 1 booking 1 landlord->tenant review

    // danh sách landlord đã đánh giá
    Page<TenantReview> findByLandlord_UserIdOrderByCreatedAtDesc(String landlordId, Pageable pageable);

    // danh sách đánh giá về một tenant (hồ sơ tenant)
    Page<TenantReview> findByTenant_UserIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    // các booking đã completed của landlord nhưng CHƯA đánh giá
    @Query("""
    select b.bookingId
    from Booking b
    where b.property.landlord.userId = :landlordId
      and b.bookingStatus = vn.edu.iuh.fit.entities.enums.BookingStatus.COMPLETED
      and b.bookingId not in (select tr.booking.bookingId from TenantReview tr)
    order by b.createdAt desc
  """)
    Page<String> findPendingBookingsForLandlord(String landlordId, Pageable pageable);
}
