package com.nexa.ingestion.service;

import com.nexa.ingestion.config.HuggingFaceProperties;
import com.nexa.ingestion.config.IngestionProperties;
import com.nexa.ingestion.dto.DocumentMetadata;
import com.nexa.ingestion.dto.SourceDocument;
import com.nexa.ingestion.dto.qdrant.QdrantPoint;
import com.nexa.ingestion.util.HtmlToPlainText;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Unified ingestion orchestrator for multiple sources.
 * Handles Confluence, Jira, GitHub, and FileSystem sources.
 */
@Service
public class UnifiedIngestionService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedIngestionService.class);

    private final ConfluenceService confluenceService;
    private final JiraService jiraService;
    private final GitHubService githubService;
    private final FileSystemService fileSystemService;
    private final SemanticChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final HuggingFaceProperties huggingFaceProperties;
    private final IngestionProperties ingestionProperties;
    private final Tika tika;

    public UnifiedIngestionService(
            ConfluenceService confluenceService,
            JiraService jiraService,
            GitHubService githubService,
            FileSystemService fileSystemService,
            SemanticChunkingService chunkingService,
            EmbeddingService embeddingService,
            QdrantService qdrantService,
            HuggingFaceProperties huggingFaceProperties,
            IngestionProperties ingestionProperties) {
        this.confluenceService = confluenceService;
        this.jiraService = jiraService;
        this.githubService = githubService;
        this.fileSystemService = fileSystemService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
        this.huggingFaceProperties = huggingFaceProperties;
        this.ingestionProperties = ingestionProperties;
        this.tika = new Tika();
    }

    /**
     * Ingests all configured sources and stores embeddings in Qdrant.
     */
    public Mono<IngestionResult> ingestAll() {
        log.info("Starting unified ingestion from all sources");
        return qdrantService.ensureCollection()
                .then(Flux.concat(
                        ingestConfluence(),
                        ingestJira(),
                        ingestGitHub(),
                        ingestFileSystem()
                ).collectList())
                .map(results -> {
                    int totalChunks = results.stream().mapToInt(r -> r.chunksProcessed).sum();
                    int totalDocuments = results.stream().mapToInt(r -> r.documentsProcessed).sum();
                    log.info("Ingestion complete: {} documents, {} chunks", totalDocuments, totalChunks);
                    return new IngestionResult(totalDocuments, totalChunks);
                });
    }

    /**
     * Ingests a specific Confluence space.
     */
    public Mono<IngestionResult> ingestConfluenceSpace(String spaceKey) {
        log.info("Ingesting Confluence space: {}", spaceKey);
        return qdrantService.ensureCollection()
                .then(ingestConfluenceSpaceInternal(spaceKey));
    }

    /**
     * Ingests all Confluence spaces configured.
     */
    public Mono<IngestionResult> ingestConfluence() {
        // For now, ingest all spaces found via API
        // In production, you'd configure specific spaces
        return Mono.just(new IngestionResult(0, 0)); // Placeholder
    }

    private Mono<IngestionResult> ingestConfluenceSpaceInternal(String spaceKey) {
        return confluenceService.getPagesInSpace(spaceKey)
                .flatMap(page -> {
                    String text = confluenceService.getPageTitleAndPlainText(page);
                    SourceDocument doc = new SourceDocument();
                    doc.setId(page.getId());
                    doc.setTitle(page.getTitle());
                    doc.setContent(text);
                    doc.setSourceType("Confluence");
                    doc.setUrl(page.getLinks() != null ? page.getLinks().getWebui() : "");
                    doc.setSecurityLevel("internal");
                    doc.setLanguage("en");
                    return processDocument(doc);
                })
                .collectList()
                .map(results -> {
                    int chunks = results.stream().mapToInt(r -> r.chunksProcessed).sum();
                    return new IngestionResult(results.size(), chunks);
                });
    }

    /**
     * Ingests Jira issues from configured projects.
     */
    public Mono<IngestionResult> ingestJira() {
        // Placeholder - configure projects in application.yml
        return Mono.just(new IngestionResult(0, 0));
    }

    /**
     * Ingests a specific Jira project.
     */
    public Mono<IngestionResult> ingestJiraProject(String projectKey) {
        log.info("Ingesting Jira project: {}", projectKey);
        return qdrantService.ensureCollection()
                .then(jiraService.getIssuesFromProject(projectKey)
                        .flatMap(this::processDocument)
                        .collectList()
                        .map(results -> {
                            int chunks = results.stream().mapToInt(r -> r.chunksProcessed).sum();
                            return new IngestionResult(results.size(), chunks);
                        }));
    }

    /**
     * Ingests GitHub repositories.
     */
    public Mono<IngestionResult> ingestGitHub() {
        log.info("Ingesting GitHub repositories");
        return qdrantService.ensureCollection()
                .then(githubService.getAllRepositoryFiles()
                        .flatMap(this::processDocument)
                        .collectList()
                        .map(results -> {
                            int chunks = results.stream().mapToInt(r -> r.chunksProcessed).sum();
                            return new IngestionResult(results.size(), chunks);
                        }));
    }

    /**
     * Ingests files from filesystem.
     */
    public Mono<IngestionResult> ingestFileSystem() {
        log.info("Ingesting filesystem");
        return qdrantService.ensureCollection()
                .then(Flux.fromStream(fileSystemService.scanFiles())
                        .flatMap(this::processDocument, 10)  // Limit concurrency
                        .collectList()
                        .map(results -> {
                            int chunks = results.stream().mapToInt(r -> r.chunksProcessed).sum();
                            return new IngestionResult(results.size(), chunks);
                        }));
    }

    // Supported file extensions for upload
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".txt", ".md", ".html", ".htm", ".pdf", ".docx"
    );

    /**
     * Ingests a single uploaded file.
     *
     * @param filePart the uploaded file
     * @return ingestion result
     */
    public Mono<IngestionResult> ingestUploadedFile(FilePart filePart) {
        String filename = filePart.filename();
        log.info("Ingesting uploaded file: {}", filename);

        String extension = getFileExtension(filename).toLowerCase();
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            log.warn("Unsupported file type: {}", extension);
            return Mono.just(new IngestionResult(0, 0));
        }

        return qdrantService.ensureCollection()
                .then(readFileContent(filePart, extension))
                .flatMap(content -> {
                    if (content == null || content.isBlank()) {
                        return Mono.just(new IngestionResult(0, 0));
                    }

                    SourceDocument doc = new SourceDocument();
                    doc.setId(UUID.randomUUID().toString());
                    doc.setTitle(filename);
                    doc.setContent(content);
                    doc.setSourceType("Upload");
                    doc.setSecurityLevel("internal");
                    doc.setLanguage("en");
                    doc.setCreatedAt(Instant.now());
                    doc.setUpdatedAt(Instant.now());

                    return processDocument(doc)
                            .map(r -> new IngestionResult(1, r.chunksProcessed));
                })
                .onErrorResume(e -> {
                    log.error("Error ingesting file {}: {}", filename, e.getMessage());
                    return Mono.just(new IngestionResult(0, 0));
                });
    }

    /**
     * Ingests multiple uploaded files.
     *
     * @param files flux of uploaded files
     * @return aggregated ingestion result
     */
    public Mono<IngestionResult> ingestUploadedFiles(Flux<FilePart> files) {
        log.info("Ingesting multiple uploaded files");
        return qdrantService.ensureCollection()
                .thenMany(files.flatMap(this::ingestUploadedFile, 5))
                .collectList()
                .map(results -> {
                    int totalDocs = results.stream().mapToInt(r -> r.documentsProcessed).sum();
                    int totalChunks = results.stream().mapToInt(r -> r.chunksProcessed).sum();
                    log.info("Uploaded files ingestion complete: {} documents, {} chunks", totalDocs, totalChunks);
                    return new IngestionResult(totalDocs, totalChunks);
                });
    }

    /**
     * Reads file content using Apache Tika.
     * Supports: .txt, .md, .html, .pdf, .docx, and many other formats.
     * Tika automatically detects file type and extracts text.
     */
    private Mono<String> readFileContent(FilePart filePart, String extension) {
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .map(bytes -> {
                    try {
                        String extensionLower = extension.toLowerCase();

                        // Use Tika for PDF and DOCX (handles font issues better than PDFBox)
                        if (".pdf".equals(extensionLower) || ".docx".equals(extensionLower)) {
                            return extractTextWithTika(bytes);
                        }

                        // Handle HTML files - convert to plain text
                        if (".html".equals(extensionLower) || ".htm".equals(extensionLower)) {
                            String content = new String(bytes, StandardCharsets.UTF_8);
                            return HtmlToPlainText.convert(content);
                        }

                        // For other text files, try Tika first (handles encoding better)
                        // Fallback to plain string if Tika fails
                        try {
                            return extractTextWithTika(bytes);
                        } catch (Exception e) {
                            log.debug("Tika extraction failed for {}, using plain text: {}", extension, e.getMessage());
                            return new String(bytes, StandardCharsets.UTF_8);
                        }
                    } catch (Exception e) {
                        log.error("Error reading file content: {}", e.getMessage(), e);
                        return "";
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error processing file: {}", e.getMessage(), e);
                    return Mono.just("");
                });
    }

    /**
     * Extracts text from files using Apache Tika.
     * Tika automatically detects file type and extracts text.
     * Handles PDF, DOCX, and many other formats without font issues.
     *
     * @param fileBytes file bytes
     * @return extracted text, empty string on error
     */
    private String extractTextWithTika(byte[] fileBytes) {
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            String text = tika.parseToString(inputStream);
            return text != null ? text.trim() : "";
        } catch (IOException | TikaException e) {
            log.warn("Error extracting text with Tika: {}", e.getMessage());
            return "";
        } catch (Exception e) {
            log.error("Unexpected error in Tika extraction: {}", e.getMessage(), e);
            return "";
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Processes a single document: chunks, generates embeddings, stores in Qdrant.
     */
    private Mono<DocumentProcessResult> processDocument(SourceDocument doc) {
        if (doc.getContent() == null || doc.getContent().isBlank()) {
            return Mono.just(new DocumentProcessResult(0));
        }

        List<String> chunks = chunkingService.chunkWithOverlap(
                doc.getContent(),
                ingestionProperties.getChunk().getTargetTokensMin(),
                ingestionProperties.getChunk().getTargetTokensMax(),
                ingestionProperties.getChunk().getOverlapTokens()
        );

        if (chunks.isEmpty()) {
            return Mono.just(new DocumentProcessResult(0));
        }

        // Generate embeddings in batches
        int batchSize = ingestionProperties.getEmbedBatchSize();
        List<Mono<List<QdrantPoint>>> batchMonos = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            List<String> batch = chunks.subList(i, end);
            final int startIdx = i;
            
            Mono<List<QdrantPoint>> batchMono = embeddingService.embedBatch(batch)
                    .index()
                    .map(tuple -> {
                        int idx = tuple.getT1().intValue();
                        float[] vector = tuple.getT2();
                        String chunkText = batch.get(idx);
                        int chunkIndex = startIdx + idx;
                        
                        DocumentMetadata metadata = buildMetadata(doc, chunkText, chunkIndex);
                        return QdrantService.toPoint(metadata, vector);
                    })
                    .collectList();
            
            batchMonos.add(batchMono);
        }

        return Flux.fromIterable(batchMonos)
                .flatMap(mono -> mono)
                .collectList()
                .flatMap(batches -> {
                    List<QdrantPoint> allPoints = batches.stream()
                            .flatMap(List::stream)
                            .toList();
                    
                    return qdrantService.upsert(allPoints)
                            .thenReturn(new DocumentProcessResult(allPoints.size()));
                })
                .onErrorResume(e -> {
                    log.error("Error processing document {}: {}", doc.getId(), e.getMessage());
                    return Mono.just(new DocumentProcessResult(0));
                });
    }

    private DocumentMetadata buildMetadata(SourceDocument doc, String chunkText, int chunkIndex) {
        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setId(generateChunkId(doc.getId(), chunkIndex));
        metadata.setText(chunkText);
        metadata.setSource(doc.getTitle());
        metadata.setSourceType(doc.getSourceType());
        metadata.setUrl(doc.getUrl());
        metadata.setAuthor(doc.getAuthor());
        metadata.setDepartment(doc.getDepartment());
        metadata.setTags(doc.getTags());
        metadata.setCreatedAt(doc.getCreatedAt() != null ? doc.getCreatedAt() : Instant.now());
        metadata.setUpdatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt() : Instant.now());
        metadata.setChunkIndex(chunkIndex);
        metadata.setEmbeddingModel(huggingFaceProperties.getModel());
        metadata.setSecurityLevel(doc.getSecurityLevel() != null ? doc.getSecurityLevel() : "internal");
        metadata.setLanguage(doc.getLanguage() != null ? doc.getLanguage() : "en");
        metadata.setPrecomputedEntities(doc.getPrecomputedEntities());
        metadata.setCustomFields(doc.getCustomFields());
        return metadata;
    }

    private String generateChunkId(String documentId, int chunkIndex) {
        return documentId + "_chunk_" + chunkIndex;
    }

    public static class IngestionResult {
        public final int documentsProcessed;
        public final int chunksProcessed;

        public IngestionResult(int documentsProcessed, int chunksProcessed) {
            this.documentsProcessed = documentsProcessed;
            this.chunksProcessed = chunksProcessed;
        }
    }

    private static class DocumentProcessResult {
        public final int chunksProcessed;

        public DocumentProcessResult(int chunksProcessed) {
            this.chunksProcessed = chunksProcessed;
        }
    }

}
