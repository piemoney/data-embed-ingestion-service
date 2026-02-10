package com.nexa.ingestion;

import com.nexa.ingestion.config.ConfluenceProperties;
import com.nexa.ingestion.config.FileSystemProperties;
import com.nexa.ingestion.config.GitHubProperties;
import com.nexa.ingestion.config.HuggingFaceProperties;
import com.nexa.ingestion.config.IngestionProperties;
import com.nexa.ingestion.config.JiraProperties;
import com.nexa.ingestion.config.LLMProperties;
import com.nexa.ingestion.config.QdrantProperties;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableConfigurationProperties({
        ConfluenceProperties.class,
        JiraProperties.class,
        GitHubProperties.class,
        FileSystemProperties.class,
        HuggingFaceProperties.class,
        QdrantProperties.class,
        IngestionProperties.class,
        LLMProperties.class
})
public class IngestionApplication {

    public static void main(String[] args) {
        // Load .env file if it exists (for java -jar execution)
        // Spring Boot reads from both environment variables and system properties
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(System.getProperty("user.dir"))
                    .ignoreIfMissing()
                    .load();
            // Set system properties from .env so Spring Boot can pick them up via ${VAR} syntax
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                // Only set if not already set (environment variables take precedence)
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            });
        } catch (Exception e) {
            // .env file not found or error loading - continue without it
            // This is fine if env vars are set externally
        }
        SpringApplication.run(IngestionApplication.class, args);
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
