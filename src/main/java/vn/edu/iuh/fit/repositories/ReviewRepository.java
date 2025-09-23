package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.fit.entities.Review;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, String> {
    Optional<Review> findByBooking_BookingId(String bookingId);

    @Query("""
        select r from Review r
        where r.booking.property.propertyId = :propertyId
    """)
    Page<Review> findByPropertyId(String propertyId, Pageable pageable);

    Page<Review> findByTenant_UserIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    boolean existsByBooking_BookingIdAndTenant_UserId(String bookingId, String tenantId);
}
