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
import vn.edu.iuh.fit.services.embedding.PropertyEmbeddingGenerator;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyEmbeddingJob {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_BATCH_ITERATIONS = 12;

    private final PropertyRepository propertyRepository;
    private final PropertyEmbeddingGenerator embeddingGenerator;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0/30 * * * *")
    public void schedule() {
        generateEmbeddings("scheduled");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        generateEmbeddings("startup");
    }

    @Transactional
    void generateEmbeddings(String trigger) {
        Pageable page = PageRequest.of(0, BATCH_SIZE);
        int totalUpdated = 0;

        for (int iteration = 0; iteration < MAX_BATCH_ITERATIONS; iteration++) {
            List<Property> batch = propertyRepository.findApprovedWithoutEmbedding(PostStatus.APPROVED, page);
            if (batch.isEmpty()) {
                break;
            }

            int updatedThisBatch = 0;
            for (Property property : batch) {
                if (hasEmbedding(property)) {
                    continue;
                }

                Optional<double[]> embedding = embeddingGenerator.generate(property);
                if (embedding.isEmpty()) {
                    continue;
                }

                try {
                    property.setEmbedding(objectMapper.writeValueAsString(embedding.get()));
                    updatedThisBatch++;
                    totalUpdated++;
                } catch (JsonProcessingException ex) {
                    log.warn("Failed to serialize embedding for property {}: {}", property.getPropertyId(), ex.getMessage());
                }
            }

            if (updatedThisBatch == 0) {
                break;
            }

            propertyRepository.flush();
        }

        if (totalUpdated > 0) {
            log.info("PropertyEmbeddingJob populated embeddings for {} properties (trigger={})", totalUpdated, trigger);
        }
    }

    private boolean hasEmbedding(Property property) {
        String embedding = property.getEmbedding();
        return embedding != null && !embedding.trim().isEmpty();
    }
}
