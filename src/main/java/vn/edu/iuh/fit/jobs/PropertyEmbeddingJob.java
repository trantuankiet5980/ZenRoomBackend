package vn.edu.iuh.fit.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.services.embedding.PropertyEmbeddingBatchService;
import vn.edu.iuh.fit.services.embedding.PropertyEmbeddingGenerator;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyEmbeddingJob {

    private final PropertyEmbeddingBatchService embeddingBatchService;

    @Scheduled(cron = "0 0/30 * * * *") //Chạy mỗi 30 phút
    public void schedule() {
        embeddingBatchService.populateEmbeddings("scheduled");
    }

    @EventListener(ApplicationReadyEvent.class) // Chạy khi ứng dụng khởi động xong
    public void onStartup() {
        embeddingBatchService.populateEmbeddings("startup");
    }
}
