package com.nexa.ingestion.service;

import com.nexa.ingestion.config.LLMProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Generates text responses using Hugging Face OpenAI-compatible API.
 * Uses: https://router.huggingface.co/v1/chat/completions
 */
@Service
public class LLMService {

    private final WebClient webClient;
    private final LLMProperties properties;

    public LLMService(WebClient.Builder webClientBuilder, 
                     LLMProperties properties) {
        this.properties = properties;
        var builder = webClientBuilder
                .baseUrl(properties.getApiUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        if (properties.getApiToken() != null && !properties.getApiToken().isBlank()) {
            builder = builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiToken());
        }
        
        this.webClient = builder.build();
    }

    /**
     * Generates a response using the LLM with the given prompt.
     * Uses OpenAI-compatible chat completions endpoint.
     *
     * @param prompt the prompt/question for the LLM
     * @return Mono of generated text response
     */
    public Mono<String> generate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return Mono.error(new IllegalArgumentException("Prompt cannot be blank"));
        }

        // OpenAI-compatible endpoint: /chat/completions (base URL already includes /v1)
        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", properties.getMaxTokens(),
                "temperature", properties.getTemperature(),
                "stream", false
        );

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    // Response format: {"choices": [{"message": {"content": "..."}}]}
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                        if (message != null) {
                            Object content = message.get("content");
                            if (content != null) {
                                return content.toString().trim();
                            }
                        }
                    }
                    throw new RuntimeException("Unexpected LLM response format: " + response);
                })
                .onErrorMap(e -> {
                    String errorMsg = String.format("LLM generation failed for model '%s': %s. " +
                            "If you see 410 Gone, the model may be deprecated. Try: meta-llama/Llama-3.1-8B-Instruct:novita, meta-llama/Llama-3.1-8B-Instruct, or google/flan-t5-large",
                            properties.getModel(), e.getMessage());
                    return new RuntimeException(errorMsg, e);
                });
    }

    /**
     * Generates a response with context (RAG pattern).
     * Combines context documents with the query into a prompt.
     *
     * @param query user query
     * @param context context documents (retrieved from vector search)
     * @return Mono of generated text response
     */
    public Mono<String> generateWithContext(String query, List<String> context) {
        if (query == null || query.isBlank()) {
            return Mono.error(new IllegalArgumentException("Query cannot be blank"));
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Based on the following context, answer the question.\n\n");
        promptBuilder.append("Context:\n");
        
        if (context != null && !context.isEmpty()) {
            for (int i = 0; i < context.size(); i++) {
                promptBuilder.append(i + 1).append(". ").append(context.get(i)).append("\n");
            }
        } else {
            promptBuilder.append("No specific context provided.\n");
        }
        
        promptBuilder.append("\nQuestion: ").append(query).append("\n\nAnswer:");

        return generate(promptBuilder.toString());
    }
}
