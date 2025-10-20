package vn.edu.iuh.fit.jobs;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.entities.Booking;
import vn.edu.iuh.fit.entities.enums.BookingStatus;
import vn.edu.iuh.fit.repositories.BookingRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingAutoCheckoutJob {
    private static final Logger logger = LoggerFactory.getLogger(BookingAutoCheckoutJob.class);
    private static final LocalTime CHECK_OUT_DEADLINE = LocalTime.of(11, 0);

    private final BookingRepository bookingRepo;

    @Scheduled(cron = "0 0/10 * * * *")
    public void autoCheckout() {
        runAutoCheckout();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        runAutoCheckout();
    }

    private void runAutoCheckout() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<Booking> candidates = bookingRepo
                .findByBookingStatusAndEndDateLessThanEqual(BookingStatus.CHECKED_IN, today);

        for (Booking booking : candidates) {
            LocalDate endDate = booking.getEndDate();
            if (endDate == null) {
                continue;
            }

            boolean shouldCheckout = endDate.isBefore(today);
            if (!shouldCheckout && endDate.isEqual(today)) {
                shouldCheckout = !now.toLocalTime().isBefore(CHECK_OUT_DEADLINE);
            }

            if (!shouldCheckout) {
                continue;
            }

            booking.setBookingStatus(BookingStatus.COMPLETED);
            booking.setUpdatedAt(now);
            bookingRepo.save(booking);
        }
    }
}
