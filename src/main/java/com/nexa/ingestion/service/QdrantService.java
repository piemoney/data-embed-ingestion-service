package com.nexa.ingestion.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.nexa.ingestion.config.QdrantProperties;
import com.nexa.ingestion.dto.DocumentMetadata;
import com.nexa.ingestion.dto.qdrant.QdrantPoint;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Stores and manages vectors in Qdrant via gRPC client.
 */
@Service
public class QdrantService {

    private static final Logger log = LoggerFactory.getLogger(QdrantService.class);

    private final QdrantClient client;
    private final QdrantProperties properties;

    public QdrantService(QdrantProperties properties) {
        this.properties = properties;
        var grpcClientBuilder = QdrantGrpcClient.newBuilder(
                properties.getHost(),
                properties.getPort(),
                properties.isUseTls()
        );
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            grpcClientBuilder.withApiKey(properties.getApiKey());
        }
        this.client = new QdrantClient(grpcClientBuilder.build());
    }

    @PreDestroy
    public void close() {
        try {
            client.close();
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Converts ListenableFuture to CompletableFuture for use with Reactor.
     */
    private <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> listenableFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        Futures.addCallback(listenableFuture, new com.google.common.util.concurrent.FutureCallback<T>() {
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

    /**
     * Ensures the collection exists with the configured vector size.
     * If recreateCollection is true, deletes and recreates it.
     */
    public Mono<Void> ensureCollection() {
        String collectionName = properties.getCollectionName();
        int size = properties.getVectorSize();

        if (properties.isRecreateCollection()) {
            return deleteCollection()
                    .then(createCollection(collectionName, size))
                    .doOnSuccess(v -> log.info("Collection '{}' recreated with vector size {}", collectionName, size));
        }
        return collectionExists(collectionName)
                .flatMap(exists -> {
                    if (exists) {
                        log.debug("Collection '{}' already exists", collectionName);
                        return Mono.<Void>empty();
                    } else {
                        log.info("Creating collection '{}' with vector size {}", collectionName, size);
                        return createCollection(collectionName, size);
                    }
                });
    }

    private Mono<Boolean> collectionExists(String name) {
        ListenableFuture<List<String>> future = client.listCollectionsAsync(Duration.ofSeconds(5));
        return Mono.fromFuture(toCompletableFuture(future))
                .map(collections -> collections.stream().anyMatch(c -> c.equals(name)))
                .onErrorReturn(false);
    }

    private Mono<Void> createCollection(String name, int vectorSize) {
        var vectorsConfig = Collections.VectorsConfig.newBuilder()
                .setParams(Collections.VectorParams.newBuilder()
                        .setSize(vectorSize)
                        .setDistance(Collections.Distance.Cosine)
                        .build())
                .build();

        var createCollection = Collections.CreateCollection.newBuilder()
                .setCollectionName(name)
                .setVectorsConfig(vectorsConfig)
                .build();

        ListenableFuture<Collections.CollectionOperationResponse> future = client.createCollectionAsync(createCollection, Duration.ofSeconds(10));
        return Mono.fromFuture(toCompletableFuture(future))
                .then();
    }

    public Mono<Void> deleteCollection() {
        ListenableFuture<Collections.CollectionOperationResponse> future = client.deleteCollectionAsync(
                properties.getCollectionName(),
                Duration.ofSeconds(10)
        );
        return Mono.fromFuture(toCompletableFuture(future))
                .then()
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * Upserts points (vectors + payload) into the configured collection.
     *
     * @param points list of points to upsert
     * @return Mono that completes when the request succeeds
     */
    public Mono<Void> upsert(List<QdrantPoint> points) {
        if (points == null || points.isEmpty()) {
            return Mono.empty();
        }

        List<Points.PointStruct> pointStructs = points.stream()
                .map(this::toPointStruct)
                .collect(Collectors.toList());

        var upsertPoints = Points.UpsertPoints.newBuilder()
                .setCollectionName(properties.getCollectionName())
                .addAllPoints(pointStructs)
                .setWait(true)
                .build();

        ListenableFuture<Points.UpdateResult> future = client.upsertAsync(upsertPoints, Duration.ofSeconds(30));
        return Mono.fromFuture(toCompletableFuture(future))
                .doOnSuccess(v -> log.debug("Upserted {} points to collection '{}'", points.size(), properties.getCollectionName()))
                .then()
                .onErrorMap(e -> {
                    String errorMsg = String.format("Failed to upsert points to collection '%s': %s", 
                            properties.getCollectionName(), e.getMessage());
                    log.error(errorMsg);
                    return new RuntimeException(errorMsg, e);
                });
    }

    private Points.PointStruct toPointStruct(QdrantPoint point) {
        Points.PointId pointId;
        try {
            // Try parsing as UUID first
            UUID uuid = UUID.fromString(point.getId());
            pointId = PointIdFactory.id(uuid);
        } catch (IllegalArgumentException e) {
            // If not a UUID, use as integer if numeric, otherwise convert string to UUID
            try {
                long idLong = Long.parseLong(point.getId());
                pointId = PointIdFactory.id(idLong);
            } catch (NumberFormatException ex) {
                // Convert string to UUID deterministically (name-based UUID v5)
                UUID stringUuid = UUID.nameUUIDFromBytes(point.getId().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                pointId = PointIdFactory.id(stringUuid);
            }
        }
        var builder = Points.PointStruct.newBuilder().setId(pointId);

        if (point.getVector() != null && point.getVector().length > 0) {
            var vectorBuilder = Points.Vector.newBuilder();
            for (float v : point.getVector()) {
                vectorBuilder.addData(v);
            }
            // For single unnamed vector collection, use setVectors with unnamed vector
            // The Vectors object should contain a single vector without a name
            builder.setVectors(Points.Vectors.newBuilder()
                    .setVector(vectorBuilder.build())
                    .build());
        }

        if (point.getPayload() != null && !point.getPayload().isEmpty()) {
            Map<String, JsonWithInt.Value> payloadMap = new java.util.HashMap<>();
            for (Map.Entry<String, Object> entry : point.getPayload().entrySet()) {
                payloadMap.put(entry.getKey(), toValue(entry.getValue()));
            }
            builder.putAllPayload(payloadMap);
        }

        return builder.build();
    }

    private JsonWithInt.Value toValue(Object obj) {
        var valueBuilder = JsonWithInt.Value.newBuilder();
        if (obj instanceof String) {
            valueBuilder.setStringValue((String) obj);
        } else if (obj instanceof Integer) {
            valueBuilder.setIntegerValue((Integer) obj);
        } else if (obj instanceof Long) {
            valueBuilder.setIntegerValue(((Long) obj).intValue());
        } else if (obj instanceof Float) {
            valueBuilder.setDoubleValue((Float) obj);
        } else if (obj instanceof Double) {
            valueBuilder.setDoubleValue((Double) obj);
        } else if (obj instanceof Boolean) {
            valueBuilder.setBoolValue((Boolean) obj);
        } else if (obj instanceof List) {
            // Convert list to JSON array string
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String json = mapper.writeValueAsString(obj);
                valueBuilder.setStringValue(json);
            } catch (Exception e) {
                valueBuilder.setStringValue(String.valueOf(obj));
            }
        } else if (obj instanceof Map) {
            // Convert map to JSON object string
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String json = mapper.writeValueAsString(obj);
                valueBuilder.setStringValue(json);
            } catch (Exception e) {
                valueBuilder.setStringValue(String.valueOf(obj));
            }
        } else {
            valueBuilder.setStringValue(String.valueOf(obj));
        }
        return valueBuilder.build();
    }

    /**
     * Builds a Qdrant point from chunk id, vector, and document metadata.
     */
    public static QdrantPoint toPoint(String chunkId, float[] vector, String pageId, String pageTitle, int chunkIndex, String text) {
        Map<String, Object> payload = Map.of(
                "page_id", pageId != null ? pageId : "",
                "page_title", pageTitle != null ? pageTitle : "",
                "chunk_index", chunkIndex,
                "text", text != null ? text : ""
        );
        return QdrantPoint.builder()
                .id(chunkId)
                .vector(vector)
                .payload(payload)
                .build();
    }

    /**
     * Builds a Qdrant point from DocumentMetadata with rich metadata.
     */
    public static QdrantPoint toPoint(DocumentMetadata metadata, float[] vector) {
        Map<String, Object> payload = new HashMap<>();
        
        if (metadata.getId() != null) payload.put("id", metadata.getId());
        if (metadata.getText() != null) payload.put("text", metadata.getText());
        if (metadata.getSource() != null) payload.put("source", metadata.getSource());
        if (metadata.getSourceType() != null) payload.put("source_type", metadata.getSourceType());
        if (metadata.getUrl() != null) payload.put("url", metadata.getUrl());
        if (metadata.getAuthor() != null) payload.put("author", metadata.getAuthor());
        if (metadata.getDepartment() != null) payload.put("department", metadata.getDepartment());
        if (metadata.getTags() != null && !metadata.getTags().isEmpty()) {
            payload.put("tags", metadata.getTags());
        }
        if (metadata.getCreatedAt() != null) {
            payload.put("created_at", metadata.getCreatedAt().toString());
        }
        if (metadata.getUpdatedAt() != null) {
            payload.put("updated_at", metadata.getUpdatedAt().toString());
        }
        payload.put("chunk_id", metadata.getChunkIndex());
        if (metadata.getEmbeddingModel() != null) {
            payload.put("embedding_model", metadata.getEmbeddingModel());
        }
        if (metadata.getSecurityLevel() != null) {
            payload.put("security_level", metadata.getSecurityLevel());
        }
        if (metadata.getLanguage() != null) {
            payload.put("language", metadata.getLanguage());
        }
        if (metadata.getPrecomputedEntities() != null && !metadata.getPrecomputedEntities().isEmpty()) {
            payload.put("precomputed_entities", metadata.getPrecomputedEntities());
        }
        if (metadata.getCustomFields() != null && !metadata.getCustomFields().isEmpty()) {
            metadata.getCustomFields().forEach((key, value) -> {
                if (value != null) {
                    payload.put("custom_" + key, value);
                }
            });
        }
        
        return QdrantPoint.builder()
                .id(metadata.getId())
                .vector(vector)
                .payload(payload)
                .build();
    }
}
