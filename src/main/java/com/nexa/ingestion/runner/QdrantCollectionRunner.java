package com.nexa.ingestion.runner;

import com.nexa.ingestion.service.QdrantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Ensures the Qdrant collection exists (or recreates it) on application startup.
 * When qdrant.recreate-collection is true, the collection is recreated with the configured vector size.
 */
@Component
public class QdrantCollectionRunner implements ApplicationRunner, Ordered {

    private static final Logger log = LoggerFactory.getLogger(QdrantCollectionRunner.class);

    private final QdrantService qdrantService;

    public QdrantCollectionRunner(QdrantService qdrantService) {
        this.qdrantService = qdrantService;
    }

    @Override
    public void run(ApplicationArguments args) {
        qdrantService.ensureCollection()
                .doOnError(e -> log.error("Failed to ensure Qdrant collection: {}", e.getMessage()))
                .block(Duration.ofSeconds(30));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
