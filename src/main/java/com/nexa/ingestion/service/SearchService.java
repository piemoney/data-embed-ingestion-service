package com.nexa.ingestion.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.nexa.ingestion.config.QdrantProperties;
import com.nexa.ingestion.dto.SearchResult;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Searches for similar vectors in Qdrant using cosine similarity.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final QdrantClient client;
    private final QdrantProperties properties;

    public SearchService(QdrantProperties properties) {
        this.properties = properties;
        var grpcClientBuilder = io.qdrant.client.QdrantGrpcClient.newBuilder(
                properties.getHost(),
                properties.getPort(),
                properties.isUseTls()
        );
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            grpcClientBuilder.withApiKey(properties.getApiKey());
        }
        this.client = new QdrantClient(grpcClientBuilder.build());
    }

    /**
     * Searches for similar vectors in Qdrant.
     *
     * @param queryVector embedding vector of the query
     * @param limit maximum number of results
     * @param scoreThreshold minimum similarity score (0.0 to 1.0)
     * @return Mono of list of search results
     */
    public Mono<List<SearchResult>> search(float[] queryVector, int limit, double scoreThreshold) {
        if (queryVector == null || queryVector.length == 0) {
            return Mono.error(new IllegalArgumentException("Query vector cannot be empty"));
        }

        // Build vector for search - Qdrant expects List<Float>
        List<Float> vectorList = new java.util.ArrayList<>();
        for (float v : queryVector) {
            vectorList.add(v);
        }

        // Use Qdrant client's search method
        var searchPoints = Points.SearchPoints.newBuilder()
                .setCollectionName(properties.getCollectionName())
                .addAllVector(vectorList)  // For single unnamed vector
                .setLimit(limit)
                .setWithPayload(Points.WithPayloadSelector.newBuilder()
                        .setEnable(true)
                        .build())
                .setScoreThreshold((float) scoreThreshold)
                .build();

        return Mono.fromFuture(() -> {
                    ListenableFuture<List<Points.ScoredPoint>> future = client.searchAsync(searchPoints, Duration.ofSeconds(10));
                    return toCompletableFuture(future);
                })
                .map(scoredPoints -> {
                    List<SearchResult> results = new ArrayList<>();
                    for (Points.ScoredPoint scoredPoint : scoredPoints) {
                        SearchResult result = extractSearchResult(scoredPoint);
                        if (result != null) {
                            results.add(result);
                        }
                    }
                    log.debug("Found {} similar documents for query", results.size());
                    return results;
                })
                .onErrorMap(e -> {
                    String errorMsg = String.format("Vector search failed: %s", e.getMessage());
                    log.error(errorMsg, e);
                    return new RuntimeException(errorMsg, e);
                });
    }

    private SearchResult extractSearchResult(Points.ScoredPoint scoredPoint) {
        try {
            Map<String, JsonWithInt.Value> payload = scoredPoint.getPayloadMap();
            if (payload == null || payload.isEmpty()) {
                return null;
            }

            String chunkId = extractString(payload, "chunk_id");
            String pageId = extractString(payload, "page_id");
            String pageTitle = extractString(payload, "page_title");
            String text = extractString(payload, "text");
            int chunkIndex = extractInt(payload, "chunk_index", 0);

            double score = scoredPoint.getScore();

            return new SearchResult(chunkId, pageId, pageTitle, chunkIndex, text, score);
        } catch (Exception e) {
            log.warn("Failed to extract search result from point: {}", e.getMessage());
            return null;
        }
    }

    private String extractString(Map<String, JsonWithInt.Value> payload, String key) {
        JsonWithInt.Value value = payload.get(key);
        if (value == null) return "";
        if (value.hasStringValue()) {
            return value.getStringValue();
        }
        return "";
    }

    private int extractInt(Map<String, JsonWithInt.Value> payload, String key, int defaultValue) {
        JsonWithInt.Value value = payload.get(key);
        if (value == null) return defaultValue;
        if (value.hasIntegerValue()) {
            return (int) value.getIntegerValue();
        }
        return defaultValue;
    }

    private <T> CompletableFuture<T> toCompletableFuture(
            ListenableFuture<T> listenableFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        Futures.addCallback(listenableFuture, 
                new FutureCallback<T>() {
                    @Override
                    public void onSuccess(T result) {
                        completableFuture.complete(result);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        completableFuture.completeExceptionally(t);
                    }
                }, java.util.concurrent.Executors.newCachedThreadPool());
        return completableFuture;
    }
}
