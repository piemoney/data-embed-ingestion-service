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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
     * Semantic search with LLM response generation (RAG).
     * Flow: Query → Embed → Vector Search → LLM Generation → Response
     *
     * @param request search request with query, limit, and score threshold
     * @return search response with similar documents and LLM-generated answer
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
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
                    // Extract text from results for context
                    List<String> contextTexts = results.stream()
                            .map(SearchResult::getText)
                            .collect(Collectors.toList());

                    // Generate LLM response with context
                    return llmService.generateWithContext(request.getQuery(), contextTexts)
                            .map(answer -> {
                                SearchResponse response = new SearchResponse(
                                        request.getQuery(),
                                        results,
                                        answer,
                                        "LLM" // Model name could come from LLMProperties
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
