package com.nexa.ingestion.runner;

import com.nexa.ingestion.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Optional: run ingestion on startup when --space=SPACE_KEY is passed.
 * Example: java -jar app.jar --space=TEAM
 * Or trigger via POST /ingest/{spaceKey} or programmatically via IngestionService.ingestSpace(spaceKey).
 */
@Component
public class IngestionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionRunner.class);

    private final IngestionService ingestionService;

    public IngestionRunner(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.getOptionValues("space") == null || args.getOptionValues("space").isEmpty()) {
            return;
        }
        String spaceKey = args.getOptionValues("space").get(0);
        if (spaceKey == null || spaceKey.isBlank()) {
            return;
        }
        log.info("Ingestion requested for space: {}", spaceKey);
        ingestionService.ingestSpace(spaceKey).block();
    }
}
