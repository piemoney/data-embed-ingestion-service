package com.nexa.ingestion.service;

import com.nexa.ingestion.dto.DocumentChunk;
import com.nexa.ingestion.dto.confluence.ConfluencePage;
import com.nexa.ingestion.dto.qdrant.QdrantPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the ingestion flow: Confluence → chunk → embed → Qdrant.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private static final int EMBED_BATCH_SIZE = 8;

    private final ConfluenceService confluenceService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;

    public IngestionService(ConfluenceService confluenceService,
                            ChunkingService chunkingService,
                            EmbeddingService embeddingService,
                            QdrantService qdrantService) {
        this.confluenceService = confluenceService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
    }

    /**
     * Ingests all pages in a Confluence space: fetch pages, extract text, chunk,
     * generate embeddings, and upsert into Qdrant.
     *
     * @param spaceKey Confluence space key (e.g. "TEAM", "DOC")
     * @return Mono that completes when ingestion is done (counts of pages and points)
     */
    public Mono<IngestionResult> ingestSpace(String spaceKey) {
        log.info("Starting ingestion for space: {}", spaceKey);

        return qdrantService.ensureCollection()
                .then(Mono.defer(() -> {
                    final long[] pageCount = {0L};
                    final long[] pointCount = {0L};
                    return confluenceService.getPagesInSpace(spaceKey)
                            .doOnNext(p -> pageCount[0]++)
                            .flatMap(this::processPageToChunks)
                            .buffer(EMBED_BATCH_SIZE)
                            .flatMap(chunkBatch -> {
                                List<String> texts = chunkBatch.stream().map(DocumentChunk::getText).toList();
                                return embeddingService.embedBatch(texts)
                                        .index()
                                        .map(tuple -> {
                                            int i = tuple.getT1().intValue();
                                            float[] vector = tuple.getT2();
                                            DocumentChunk chunk = chunkBatch.get(i);
                                            return QdrantService.toPoint(
                                                    chunk.getChunkId(),
                                                    vector,
                                                    chunk.getPageId(),
                                                    chunk.getPageTitle(),
                                                    chunk.getChunkIndex(),
                                                    chunk.getText()
                                            );
                                        });
                            })
                            .buffer(50)
                            .doOnNext(batch -> pointCount[0] += batch.size())
                            .flatMap((List<QdrantPoint> batch) -> qdrantService.upsert(batch))
                            .then(Mono.fromCallable(() -> new IngestionResult(pageCount[0], pointCount[0])));
                }))
                .doOnSuccess((IngestionResult r) -> log.info("Ingestion completed for space {}: {} pages, {} points upserted",
                        spaceKey, r.pagesProcessed(), r.pointsUpserted()))
                .onErrorResume(e -> {
                    log.error("Ingestion failed for space {}: {}", spaceKey, e.getMessage());
                    return Mono.error(e);
                });
    }

    /**
     * Processes a single page: extract text, chunk, embed, upsert.
     */
    public Mono<Void> processPage(ConfluencePage page) {
        String plainText = confluenceService.getPageTitleAndPlainText(page);
        List<DocumentChunk> chunks = chunkingService.chunk(page.getId(), page.getTitle(), plainText);
        if (chunks.isEmpty()) {
            log.debug("No chunks for page: {} ({})", page.getTitle(), page.getId());
            return Mono.empty();
        }
        return embedChunksAndUpsert(chunks);
    }

    /**
     * Converts a page to chunks (for use in flux that batches upserts).
     */
    public Flux<DocumentChunk> processPageToChunks(ConfluencePage page) {
        String plainText = confluenceService.getPageTitleAndPlainText(page);
        List<DocumentChunk> chunks = chunkingService.chunk(page.getId(), page.getTitle(), plainText);
        return Flux.fromIterable(chunks);
    }

    /**
     * Embeds chunks and converts to Qdrant points (no upsert).
     */
    public Flux<QdrantPoint> chunksToPoints(Flux<DocumentChunk> chunks) {
        return chunks
                .buffer(EMBED_BATCH_SIZE)
                .flatMap(batch -> {
                    List<String> texts = batch.stream().map(DocumentChunk::getText).toList();
                    return embeddingService.embedBatch(texts)
                            .index()
                            .map(tuple -> {
                                int i = tuple.getT1().intValue();
                                float[] vector = tuple.getT2();
                                DocumentChunk chunk = batch.get(i);
                                return QdrantService.toPoint(
                                        chunk.getChunkId(),
                                        vector,
                                        chunk.getPageId(),
                                        chunk.getPageTitle(),
                                        chunk.getChunkIndex(),
                                        chunk.getText()
                                );
                            });
                });
    }

    private Mono<Void> embedChunksAndUpsert(List<DocumentChunk> chunks) {
        List<String> texts = chunks.stream().map(DocumentChunk::getText).toList();
        List<QdrantPoint> points = new ArrayList<>();
        return embeddingService.embedBatch(texts)
                .index()
                .map(tuple -> {
                    int i = tuple.getT1().intValue();
                    float[] vector = tuple.getT2();
                    DocumentChunk chunk = chunks.get(i);
                    return QdrantService.toPoint(
                            chunk.getChunkId(),
                            vector,
                            chunk.getPageId(),
                            chunk.getPageTitle(),
                            chunk.getChunkIndex(),
                            chunk.getText()
                    );
                })
                .collectList()
                .flatMap(qdrantService::upsert);
    }

    /**
     * Result of an ingestion run.
     */
    public record IngestionResult(long pagesProcessed, long pointsUpserted) {}
}
