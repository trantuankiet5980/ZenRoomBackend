package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.entities.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    Optional<Invoice> findByBooking_BookingId(String bookingId);
    Optional<Invoice> findByInvoiceNo(String invoiceNo);
    Optional<Invoice> findByPaymentRef(String paymentRef);

    @Query("select coalesce(max(i.invoiceNo), null) from Invoice i where i.invoiceNo like concat(:prefix, '%')")
    String findMaxInvoiceNoWithPrefix(@Param("prefix") String prefix);

    @Query("""
        select i from Invoice i
        where i.booking.bookingId = :bookingId and i.status = vn.edu.iuh.fit.entities.enums.InvoiceStatus.ISSUED
    """)
    List<Invoice> findIssuedByBooking(@Param("bookingId") String bookingId);

    @Query("""
        select i from Invoice i
        where i.booking.bookingId = :bookingId and i.status = vn.edu.iuh.fit.entities.enums.InvoiceStatus.PAID
    """)
    List<Invoice> findPaidByBooking(@Param("bookingId") String bookingId);

    @Query("""
        select i from Invoice i
        where i.booking.property.landlord.userId = :landlordId
          and i.status = vn.edu.iuh.fit.entities.enums.InvoiceStatus.PAID
          and function('DATE', i.paidAt) = :date
    """)
    List<Invoice> findPaidByLandlordAndDate(
            @Param("landlordId") String landlordId,
            @Param("date") LocalDate date
    );

    Page<Invoice> findByBooking_Tenant_UserIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Page<Invoice> findByBooking_Property_Landlord_UserIdOrderByCreatedAtDesc(String landlordId, Pageable pageable);

    Optional<Invoice> findByInvoiceIdAndBooking_Tenant_UserId(String invoiceId, String tenantId);

    Optional<Invoice> findByInvoiceIdAndBooking_Property_Landlord_UserId(String invoiceId, String landlordId);

    Optional<Invoice> findByBooking_BookingIdAndBooking_Tenant_UserId(String bookingId, String tenantId);

    Optional<Invoice> findByBooking_BookingIdAndBooking_Property_Landlord_UserId(String bookingId, String landlordId);

    @Query("""
        select i from Invoice i
        where (:status is null or i.status = :status)
            and (:createdFrom is null or function('DATE', i.createdAt) >= :createdFrom)
            and (:createdTo is null or function('DATE', i.createdAt) <= :createdTo)
    """)
    Page<Invoice> findAllForAdmin(@Param("status") InvoiceStatus status,
                                  @Param("createdFrom") LocalDate createdFrom,
                                  @Param("createdTo") LocalDate createdTo,
                                  Pageable pageable);


    @Query("""
    SELECT
        i.booking.property.landlord.userId,
        COALESCE(i.booking.property.landlord.fullName, 'Unknown'),
        FUNCTION('DATE', i.paidAt),
        SUM(CASE WHEN i.status = 'PAID' THEN i.total ELSE 0 END),
        SUM(CASE WHEN i.status = 'PAID' THEN i.platformFee ELSE 0 END),
        SUM(CASE WHEN i.status = 'PAID' THEN i.landlordReceivable ELSE 0 END),
        SUM(CASE WHEN i.status = 'REFUNDED' THEN COALESCE(i.refundableAmount, 0) ELSE 0 END)
    FROM Invoice i
    WHERE i.paidAt IS NOT NULL
      AND i.status = 'PAID'
      AND (:from IS NULL OR FUNCTION('DATE', i.paidAt) >= :from)
      AND (:to IS NULL OR FUNCTION('DATE', i.paidAt) <= :to)
    GROUP BY i.booking.property.landlord.userId,
             i.booking.property.landlord.fullName,
             FUNCTION('DATE', i.paidAt)
    ORDER BY FUNCTION('DATE', i.paidAt) DESC
    """)
    List<Object[]> getLandlordRevenueByDayRaw(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
    SELECT
        i.booking.property.landlord.userId,
        COALESCE(i.booking.property.landlord.fullName, 'Unknown'),
        FUNCTION('YEAR', i.paidAt),
        FUNCTION('MONTH', i.paidAt),
        SUM(CASE WHEN i.status = 'PAID' THEN i.total ELSE 0 END),
        SUM(CASE WHEN i.status = 'PAID' THEN i.platformFee ELSE 0 END),
        SUM(CASE WHEN i.status = 'PAID' THEN i.landlordReceivable ELSE 0 END),
        SUM(CASE WHEN i.status = 'REFUNDED' THEN COALESCE(i.refundableAmount, 0) ELSE 0 END)
    FROM Invoice i
    WHERE i.paidAt IS NOT NULL
      AND i.status = 'PAID'
      AND (:year IS NULL OR FUNCTION('YEAR', i.paidAt) = :year)
      AND (:month IS NULL OR FUNCTION('MONTH', i.paidAt) = :month)
    GROUP BY i.booking.property.landlord.userId,
             i.booking.property.landlord.fullName,
             FUNCTION('YEAR', i.paidAt), FUNCTION('MONTH', i.paidAt)
    ORDER BY FUNCTION('YEAR', i.paidAt) DESC, FUNCTION('MONTH', i.paidAt) DESC
    """)
    List<Object[]> getLandlordRevenueByMonthRaw(
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query("""
    SELECT 
        i.booking.property.landlord.userId,
        COALESCE(i.booking.property.landlord.fullName, 'Unknown'),
        FUNCTION('YEAR', i.paidAt),
        SUM(CASE WHEN i.status = 'PAID' THEN i.total ELSE 0 END),
        SUM(CASE WHEN i.status = 'PAID' THEN i.platformFee ELSE 0 END),
        SUM(CASE WHEN i.status = 'PAID' THEN i.landlordReceivable ELSE 0 END),
        SUM(CASE WHEN i.status = 'REFUNDED' THEN COALESCE(i.refundableAmount, 0) ELSE 0 END)
    FROM Invoice i
    WHERE i.paidAt IS NOT NULL
      AND i.status = 'PAID'
      AND (:year IS NULL OR FUNCTION('YEAR', i.paidAt) = :year)
    GROUP BY i.booking.property.landlord.userId,
             i.booking.property.landlord.fullName,
             FUNCTION('YEAR', i.paidAt)
    ORDER BY FUNCTION('YEAR', i.paidAt) DESC
    """)
    List<Object[]> getLandlordRevenueByYearRaw(
            @Param("year") Integer year
    );

    @Query("""
    SELECT COALESCE(SUM(i.landlordReceivable), 0)
    FROM Invoice i
    WHERE i.paidAt IS NOT NULL
      AND i.status = 'PAID'
      AND i.booking.property.landlord.userId = :landlordId
      AND FUNCTION('YEAR', i.paidAt) = :year
      AND FUNCTION('MONTH', i.paidAt) = :month
    """)
    BigDecimal getLandlordPayoutForMonth(
            @Param("landlordId") String landlordId,
            @Param("year") int year,
            @Param("month") int month
    );

    @Query("""
    SELECT FUNCTION('DATE', i.paidAt), COALESCE(SUM(i.landlordReceivable), 0)
    FROM Invoice i
    WHERE i.paidAt IS NOT NULL
      AND i.status = 'PAID'
      AND i.booking.property.landlord.userId = :landlordId
      AND FUNCTION('YEAR', i.paidAt) = :year
      AND FUNCTION('MONTH', i.paidAt) = :month
    GROUP BY FUNCTION('DATE', i.paidAt)
    ORDER BY FUNCTION('DATE', i.paidAt)
    """)
    List<Object[]> getLandlordPayoutDailyBreakdown(
            @Param("landlordId") String landlordId,
            @Param("year") int year,
            @Param("month") int month
    );

    @Query("""
    SELECT COALESCE(SUM(i.landlordReceivable), 0)
    FROM Invoice i
    WHERE i.paidAt IS NOT NULL
      AND i.status = 'PAID'
      AND i.booking.property.landlord.userId = :landlordId
      AND FUNCTION('YEAR', i.paidAt) = :year
    """)
    BigDecimal getLandlordPayoutForYear(
            @Param("landlordId") String landlordId,
            @Param("year") int year
    );

    @Query("""
    SELECT FUNCTION('YEAR', i.paidAt), FUNCTION('MONTH', i.paidAt), COALESCE(SUM(i.landlordReceivable), 0)
    FROM Invoice i
    WHERE i.paidAt IS NOT NULL
      AND i.status = 'PAID'
      AND i.booking.property.landlord.userId = :landlordId
      AND FUNCTION('YEAR', i.paidAt) = :year
    GROUP BY FUNCTION('YEAR', i.paidAt), FUNCTION('MONTH', i.paidAt)
    ORDER BY FUNCTION('YEAR', i.paidAt), FUNCTION('MONTH', i.paidAt)
    """)
    List<Object[]> getLandlordPayoutMonthlyBreakdown(
            @Param("landlordId") String landlordId,
            @Param("year") int year
    );

    @Query("""
    SELECT
        l.userId,
        COALESCE(l.fullName, 'Unknown'),
        COALESCE(l.phoneNumber, ''),
        FUNCTION('MONTH', i.paidAt) AS month,
        COALESCE(
            SUM(CASE WHEN i.status = 'PAID' THEN i.landlordReceivable ELSE 0 END)
            - SUM(CASE WHEN i.status = 'REFUNDED' THEN COALESCE(i.refundableAmount, 0) ELSE 0 END),
            0
        )
    FROM Invoice i
    JOIN i.booking.property.landlord l
    WHERE i.paidAt IS NOT NULL
      AND FUNCTION('YEAR', i.paidAt) = :year
    GROUP BY l.userId, l.fullName, l.phoneNumber, FUNCTION('MONTH', i.paidAt)
    ORDER BY l.userId, FUNCTION('MONTH', i.paidAt)
    """)
    List<Object[]> getLandlordPayoutByMonthForYear(@Param("year") int year);
}