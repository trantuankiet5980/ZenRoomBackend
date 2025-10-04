package vn.edu.iuh.fit.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.services.SearchSuggestionService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSuggestionBatchJob {
    private final SearchSuggestionService searchSuggestionService;

    @Scheduled(cron = "0 */15 * * * *")
    public void scheduleRebuild() {
        try {
            searchSuggestionService.rebuildPropertySuggestions();
        } catch (Exception ex) {
            log.error("Failed to rebuild property search suggestions", ex);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            searchSuggestionService.rebuildPropertySuggestions();
        } catch (Exception ex) {
            log.error("Failed to rebuild property search suggestions on startup", ex);
        }
    }
}
