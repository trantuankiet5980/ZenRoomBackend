package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.fit.entities.Invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    Optional<Invoice> findByBooking_BookingId(String bookingId);
    Optional<Invoice> findByInvoiceNo(String invoiceNo);

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
}
