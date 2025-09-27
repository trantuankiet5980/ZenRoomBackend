package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.fit.entities.Booking;
import vn.edu.iuh.fit.entities.enums.BookingStatus;

import java.time.LocalDateTime;

public interface BookingRepository extends JpaRepository<Booking, String> {
    // Kiểm tra overlap khi thuê theo ngày (các booking chưa bị hủy/hoàn tất)
    @Query("""
        select count(b) > 0 from Booking b
        where b.property.propertyId = :propertyId
          and b.bookingStatus not in (vn.edu.iuh.fit.entities.enums.BookingStatus.CANCELLED,
                               vn.edu.iuh.fit.entities.enums.BookingStatus.REJECTED,
                               vn.edu.iuh.fit.entities.enums.BookingStatus.COMPLETED)
          and b.startDate < :endDate and b.endDate > :startDate
    """)
    boolean existsOverlap(@Param("propertyId") String propertyId,
                          @Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    Page<Booking> findByTenant_UserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Page<Booking> findByProperty_Landlord_UserIdOrderByCreatedAtDesc(String landlordId, Pageable pageable);

    Page<Booking> findByTenant_UserIdAndBookingStatusOrderByCreatedAtDesc(
            String userId,
            BookingStatus status,
            Pageable pageable
    );

    Page<Booking> findByProperty_Landlord_UserIdAndBookingStatusOrderByCreatedAtDesc(
            String landlordId,
            BookingStatus status,
            Pageable pageable
    );
}
