package vn.edu.iuh.fit.jobs;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.repositories.UserRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UserPurgeJob {
    private final UserRepository userRepo;

    @Scheduled(cron = "0 0 2 * * *") // chạy 2h sáng hằng ngày
    public void purgeDeletedUsers() {
        runPurge();
    }

    @EventListener(ApplicationReadyEvent.class) // chạy ngay khi app start
    public void runOnStartup() {
        runPurge();
    }

    private void runPurge() {
        var now = LocalDateTime.now();
        userRepo.findAllEligibleForHardDelete(now)
                .forEach(userRepo::delete);
    }
}
