package com.nexa.ingestion.web;

import com.nexa.ingestion.dto.ChatRequest;
import com.nexa.ingestion.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * REST API for chat with streaming responses.
 * POST /chat - Stream chat responses using Server-Sent Events (SSE)
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Chat endpoint with streaming response using Server-Sent Events (SSE).
     * Simulates ChatGPT-style streaming with chunk-by-chunk delivery.
     *
     * @param request chat request with user query
     * @return Flux of Server-Sent Events containing text chunks
     *
     * Example curl:
     * curl -N -X POST http://localhost:8080/api/chat \
     *   -H "Content-Type: application/json" \
     *   -d '{"query": "What is Section 80C?"}'
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request received: query='{}'", request.getQuery());

        return chatService.streamResponse(request.getQuery())
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("[DONE]")
                                .build()
                ))
                .onErrorResume(e -> {
                    log.error("Error in chat stream", e);
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data("Error: " + e.getMessage())
                                    .build()
                    );
                });
    }

    /**
     * Alternative endpoint returning plain text stream (without SSE wrapper).
     * Useful for simpler frontend implementations.
     *
     * @param request chat request with user query
     * @return Flux of text chunks
     *
     * Example curl:
     * curl -N -X POST http://localhost:8080/api/chat/text \
     *   -H "Content-Type: application/json" \
     *   -d '{"query": "What is Section 80C?"}'
     */
    @PostMapping(value = "/text", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chatText(@Valid @RequestBody ChatRequest request) {
        log.info("Chat text request received: query='{}'", request.getQuery());

        return chatService.streamResponse(request.getQuery())
                .onErrorResume(e -> {
                    log.error("Error in chat stream", e);
                    return Flux.just("Error: " + e.getMessage());
                });
    }
}
