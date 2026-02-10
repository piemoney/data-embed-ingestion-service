package com.nexa.ingestion.service;

import com.nexa.ingestion.config.HuggingFaceProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Generates embeddings via Hugging Face Inference API router.
 * Uses: https://router.huggingface.co/hf-inference/models/{model}
 */
@Service
public class EmbeddingService {

    private final WebClient webClient;
    private final HuggingFaceProperties properties;

    public EmbeddingService(WebClient.Builder webClientBuilder,
                            HuggingFaceProperties properties) {
        this.properties = properties;
        var builder = webClientBuilder
                .baseUrl(properties.getApiUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        // Add authorization header - required for Inference API
        if (properties.getApiToken() != null && !properties.getApiToken().isBlank()) {
            builder = builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiToken());
        }
        
        this.webClient = builder.build();
    }

    /**
     * Embeds a single text and returns the vector.
     * Uses Hugging Face Inference API router endpoint.
     *
     * @param text input text
     * @return Mono of embedding vector (float array)
     */
    public Mono<float[]> embed(String text) {
        if (text == null || text.isBlank()) {
            return Mono.error(new IllegalArgumentException("Text must not be blank"));
        }
        
        String modelPath = "/hf-inference/models/" + properties.getModel();
        
        return webClient.post()
                .uri(modelPath)
                .bodyValue(Map.of("inputs", text))
                .retrieve()
                .bodyToMono(List.class)
                .map(list -> {
                    // Response format: [[float...]] - array of arrays, take first
                    if (list != null && !list.isEmpty() && list.get(0) instanceof List) {
                        return toFloatArray((List<?>) list.get(0));
                    }
                    // Fallback: direct array
                    return toFloatArray(list);
                })
                .onErrorMap(e -> {
                    String errorMsg = String.format("Hugging Face embedding failed at %s: %s", 
                            modelPath, e.getMessage());
                    return new RuntimeException(errorMsg, e);
                });
    }

    /**
     * Embeds multiple texts in one request (batch).
     * Uses Hugging Face Inference API router endpoint.
     *
     * @param texts list of texts to embed
     * @return Flux of embedding vectors in same order as input
     */
    public Flux<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Flux.empty();
        }
        
        String modelPath = "/hf-inference/models/" + properties.getModel();
        
        return webClient.post()
                .uri(modelPath)
                .bodyValue(Map.of("inputs", texts))
                .retrieve()
                .bodyToMono(List.class)
                .flatMapMany(list -> {
                    // Response format: [[embedding1], [embedding2], ...]
                    if (list == null) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(list)
                            .map(item -> {
                                if (item instanceof List) {
                                    return toFloatArray((List<?>) item);
                                }
                                return new float[0];
                            });
                })
                .onErrorResume(e -> {
                    // Fallback to sequential single-text calls if batch fails
                    return Flux.fromIterable(texts)
                            .flatMap(this::embed, 1);
                });
    }

    private static float[] toFloatArray(List<?> list) {
        if (list == null) return new float[0];
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            if (o instanceof Number) {
                arr[i] = ((Number) o).floatValue();
            }
        }
        return arr;
    }
}
