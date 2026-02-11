package com.nexa.ingestion.web;

import com.nexa.ingestion.dto.SearchRequest;
import com.nexa.ingestion.dto.SearchResponse;
import com.nexa.ingestion.dto.SearchResult;
import com.nexa.ingestion.service.EmbeddingService;
import com.nexa.ingestion.service.LLMService;
import com.nexa.ingestion.service.SearchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for semantic search with LLM-powered responses (RAG).
 * POST /api/search - Search vector DB and generate LLM response
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final EmbeddingService embeddingService;
    private final SearchService searchService;
    private final LLMService llmService;

    public SearchController(EmbeddingService embeddingService,
                           SearchService searchService,
                           LLMService llmService) {
        this.embeddingService = embeddingService;
        this.searchService = searchService;
        this.llmService = llmService;
    }

    /**
     * Search endpoint with streaming response using Server-Sent Events (SSE).
     * Simulates ChatGPT-style streaming with chunk-by-chunk delivery.
     * RAG flow: embed query → vector search → LLM with context → stream answer chunks.
     *
     * @param request search request with query, limit, and score threshold
     * @return Flux of Server-Sent Events containing answer text chunks
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> searchStream(@Valid @RequestBody SearchRequest request) {
        log.info("Search stream request: query='{}', limit={}, threshold={}",
                request.getQuery(), request.getLimit(), request.getScoreThreshold());

        return embeddingService.embed(request.getQuery())
                .flatMap(queryVector -> searchService.search(
                        queryVector,
                        request.getLimit(),
                        request.getScoreThreshold()
                ))
                .flatMapMany(results -> {
                    List<String> contextTexts = results.stream()
                            .map(SearchResult::getText)
                            .collect(Collectors.toList());
                    return llmService.generateWithContext(request.getQuery(), contextTexts)
                            .flatMapMany(fullAnswer -> streamAnswerChunks(fullAnswer));
                })
                .onErrorResume(e -> {
                    log.error("Search stream failed: {}", e.getMessage(), e);
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data("Error: " + e.getMessage())
                                    .build()
                    );
                });
    }

    /**
     * Streams the full answer as word-by-word chunks with delay (ChatGPT-style).
     */
    private static Flux<ServerSentEvent<String>> streamAnswerChunks(String fullAnswer) {
        if (fullAnswer == null || fullAnswer.isBlank()) {
            return Flux.just(ServerSentEvent.<String>builder().event("done").data("[DONE]").build());
        }
        String[] words = fullAnswer.trim().split("\\s+");
        return Flux.fromArray(words)
                .index()
                .concatMap(tuple -> {
                    long index = tuple.getT1();
                    String word = tuple.getT2();
                    String chunk = word + (index < words.length - 1 ? " " : "");
                    return Flux.just(ServerSentEvent.<String>builder().data(chunk).build())
                            .delayElements(Duration.ofMillis(50));
                })
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("[DONE]")
                                .build()
                ));
    }

    /**
     * Search endpoint with JSON response (non-streaming).
     *
     * @param request search request with query, limit, and score threshold
     * @return search response with similar documents and LLM-generated answer
     */
    @PostMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<SearchResponse>> search(@Valid @RequestBody SearchRequest request) {
        log.info("Search request: query='{}', limit={}, threshold={}",
                request.getQuery(), request.getLimit(), request.getScoreThreshold());

        return embeddingService.embed(request.getQuery())
                .flatMap(queryVector -> searchService.search(
                        queryVector,
                        request.getLimit(),
                        request.getScoreThreshold()
                ))
                .flatMap(results -> {
                    List<String> contextTexts = results.stream()
                            .map(SearchResult::getText)
                            .collect(Collectors.toList());
                    return llmService.generateWithContext(request.getQuery(), contextTexts)
                            .map(answer -> {
                                SearchResponse response = new SearchResponse(
                                        request.getQuery(),
                                        results,
                                        answer,
                                        "LLM"
                                );
                                return ResponseEntity.ok(response);
                            });
                })
                .onErrorResume(e -> {
                    log.error("Search failed: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(new SearchResponse(request.getQuery(), List.of(),
                                    "Error: " + e.getMessage(), null)));
                });
    }
}
