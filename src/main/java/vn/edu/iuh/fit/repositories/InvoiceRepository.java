package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.entities.enums.InvoiceStatus;

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
}
