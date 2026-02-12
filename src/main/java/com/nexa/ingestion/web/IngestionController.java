package com.nexa.ingestion.web;

import com.nexa.ingestion.service.UnifiedIngestionService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST endpoints for unified ingestion from multiple sources.
 */
@RestController
@RequestMapping("/api/ingest")
public class IngestionController {

    private final UnifiedIngestionService unifiedIngestionService;

    public IngestionController(UnifiedIngestionService unifiedIngestionService) {
        this.unifiedIngestionService = unifiedIngestionService;
    }

    /**
     * Ingest all configured sources.
     * POST /api/ingest/all
     */
    @PostMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<IngestionResultDto> ingestAll() {
        return unifiedIngestionService.ingestAll()
                .map(r -> new IngestionResultDto(r.documentsProcessed, r.chunksProcessed));
    }

    /**
     * Ingest a Confluence space.
     * POST /api/ingest/confluence/{spaceKey}
     */
    @PostMapping(value = "/confluence/{spaceKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<IngestionResultDto> ingestConfluenceSpace(@PathVariable String spaceKey) {
        return unifiedIngestionService.ingestConfluenceSpace(spaceKey)
                .map(r -> new IngestionResultDto(r.documentsProcessed, r.chunksProcessed));
    }

    /**
     * Ingest a Jira project.
     * POST /api/ingest/jira/{projectKey}
     */
    @PostMapping(value = "/jira/{projectKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<IngestionResultDto> ingestJiraProject(@PathVariable String projectKey) {
        return unifiedIngestionService.ingestJiraProject(projectKey)
                .map(r -> new IngestionResultDto(r.documentsProcessed, r.chunksProcessed));
    }

    /**
     * Ingest GitHub repositories.
     * POST /api/ingest/github
     */
    @PostMapping(value = "/github", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<IngestionResultDto> ingestGitHub() {
        return unifiedIngestionService.ingestGitHub()
                .map(r -> new IngestionResultDto(r.documentsProcessed, r.chunksProcessed));
    }

    /**
     * Ingest filesystem.
     * POST /api/ingest/filesystem
     */
    @PostMapping(value = "/filesystem", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<IngestionResultDto> ingestFileSystem() {
        return unifiedIngestionService.ingestFileSystem()
                .map(r -> new IngestionResultDto(r.documentsProcessed, r.chunksProcessed));
    }

    /**
     * Ingest a single uploaded file.
     * POST /api/ingest/file
     * Content-Type: multipart/form-data
     *
     * Supports: .txt, .md, .html, .pdf, .docx
     *
     * Example curl:
     * curl -X POST http://localhost:8080/api/ingest/file \
     *   -F "file=@/path/to/document.txt"
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<IngestionResultDto> ingestFile(@RequestPart("file") Mono<FilePart> fileMono) {
        return fileMono.flatMap(unifiedIngestionService::ingestUploadedFile)
                .map(r -> new IngestionResultDto(r.documentsProcessed, r.chunksProcessed));
    }

    /**
     * Ingest multiple uploaded files.
     * POST /api/ingest/files
     * Content-Type: multipart/form-data
     *
     * Example curl:
     * curl -X POST http://localhost:8080/api/ingest/files \
     *   -F "files=@doc1.txt" -F "files=@doc2.md"
     */
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<IngestionResultDto> ingestFiles(@RequestPart("files") Flux<FilePart> files) {
        return unifiedIngestionService.ingestUploadedFiles(files)
                .map(r -> new IngestionResultDto(r.documentsProcessed, r.chunksProcessed));
    }

    public record IngestionResultDto(int documentsProcessed, int chunksProcessed) {}
}
