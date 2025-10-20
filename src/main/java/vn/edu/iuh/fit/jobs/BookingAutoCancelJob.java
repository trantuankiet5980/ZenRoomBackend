package vn.edu.iuh.fit.jobs;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.entities.Booking;
import vn.edu.iuh.fit.entities.Invoice;
import vn.edu.iuh.fit.entities.enums.BookingStatus;
import vn.edu.iuh.fit.entities.enums.ContractStatus;
import vn.edu.iuh.fit.entities.enums.InvoiceStatus;
import vn.edu.iuh.fit.repositories.BookingRepository;
import vn.edu.iuh.fit.repositories.ContractRepository;
import vn.edu.iuh.fit.repositories.InvoiceRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BookingAutoCancelJob {
    private static final Logger logger = LoggerFactory.getLogger(BookingAutoCancelJob.class);
    private static final Duration PAYMENT_WINDOW = Duration.ofHours(24);
    private static final Set<BookingStatus> CANCELLABLE_STATUSES =
            EnumSet.of(BookingStatus.PENDING_PAYMENT, BookingStatus.AWAITING_LANDLORD_APPROVAL);

    private final BookingRepository bookingRepo;
    private final InvoiceRepository invoiceRepo;
    private final ContractRepository contractRepo;

    @Scheduled(cron = "0 0/10 * * * *")
    public void autoCancel() {
        runAutoCancel();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        runAutoCancel();
    }

    private void runAutoCancel() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minus(PAYMENT_WINDOW);

        List<Booking> overdueBookings = bookingRepo
                .findByBookingStatusInAndCreatedAtBefore(CANCELLABLE_STATUSES, threshold);

        for (Booking booking : overdueBookings) {
            if (booking == null) {
                continue;
            }

            Invoice invoice = invoiceRepo.findByBooking_BookingId(booking.getBookingId()).orElse(null);
            if (invoice != null && invoice.getStatus() == InvoiceStatus.PAID) {
                continue;
            }

            booking.setBookingStatus(BookingStatus.CANCELLED);
            booking.setUpdatedAt(now);
            bookingRepo.save(booking);

            if (invoice != null && invoice.getStatus() != InvoiceStatus.VOID) {
                invoice.setStatus(InvoiceStatus.VOID);
                invoice.setUpdatedAt(now);
                invoiceRepo.save(invoice);
            }

            contractRepo.findByBooking_BookingId(booking.getBookingId()).ifPresent(contract -> {
                contract.setContractStatus(ContractStatus.CANCELLED);
                contract.setUpdatedAt(now);
                contractRepo.save(contract);
            });
        }
    }
}
