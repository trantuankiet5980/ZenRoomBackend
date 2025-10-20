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
public class BookingAutoCheckinJob {
    private static final Logger logger = LoggerFactory.getLogger(BookingAutoCheckinJob.class);
    private static final LocalTime STANDARD_CHECK_IN_TIME = LocalTime.of(14, 0);
    private static final LocalTime AUTO_CHECK_IN_THRESHOLD = LocalTime.of(14, 30);

    private final BookingRepository bookingRepo;

    @Scheduled(cron = "0 0/10 * * * *")
    public void autoCheckIn() {
        runAutoCheckIn();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        runAutoCheckIn();
    }

    private void runAutoCheckIn() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();

        if (currentTime.isBefore(AUTO_CHECK_IN_THRESHOLD)) {
            return;
        }

        List<Booking> candidates = bookingRepo
                .findByBookingStatusAndStartDate(BookingStatus.APPROVED, today);

        for (Booking booking : candidates) {
            if (booking.getStartDate() == null) {
                continue;
            }

            LocalDateTime standardCheckIn = booking.getStartDate().atTime(STANDARD_CHECK_IN_TIME);
            if (now.isBefore(standardCheckIn)) {
                continue;
            }

            booking.setBookingStatus(BookingStatus.CHECKED_IN);
            booking.setUpdatedAt(now);
            bookingRepo.save(booking);
        }
    }
}
