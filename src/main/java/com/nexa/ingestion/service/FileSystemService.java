package com.nexa.ingestion.service;

import com.nexa.ingestion.config.FileSystemProperties;
import com.nexa.ingestion.dto.SourceDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reads files from the local filesystem.
 */
@Service
public class FileSystemService {

    private final FileSystemProperties properties;

    public FileSystemService(FileSystemProperties properties) {
        this.properties = properties;
    }

    /**
     * Scans configured base paths and returns all matching files as SourceDocument.
     */
    public Stream<SourceDocument> scanFiles() {
        return properties.getBasePaths().stream()
                .flatMap(this::scanDirectory)
                .filter(doc -> doc != null && doc.getContent() != null && !doc.getContent().isBlank());
    }

    private Stream<SourceDocument> scanDirectory(String basePath) {
        try {
            Path path = Paths.get(basePath);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return Stream.empty();
            }
            
            Stream<Path> fileStream = properties.isRecursive() 
                    ? Files.walk(path)
                    : Files.list(path);
            
            return fileStream
                    .filter(Files::isRegularFile)
                    .filter(this::shouldProcessFile)
                    .map(this::readFile)
                    .filter(doc -> doc != null);
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private boolean shouldProcessFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return properties.getFileExtensions().stream()
                .anyMatch(ext -> fileName.endsWith(ext.toLowerCase()));
    }

    private SourceDocument readFile(Path path) {
        try {
            long sizeBytes = Files.size(path);
            if (sizeBytes > properties.getMaxFileSizeKb() * 1024L) {
                return null;
            }
            
            String content = Files.readString(path);
            String fileName = path.getFileName().toString();
            String parentDir = path.getParent() != null ? path.getParent().getFileName().toString() : "";
            
            // Infer source type from directory
            String sourceType = inferSourceType(parentDir);
            String department = inferDepartment(parentDir);
            
            SourceDocument doc = new SourceDocument();
            doc.setId(path.toAbsolutePath().toString());
            doc.setTitle(fileName);
            doc.setContent(content);
            doc.setSourceType(sourceType);
            doc.setUrl("file://" + path.toAbsolutePath());
            doc.setDepartment(department);
            doc.setSecurityLevel("internal");
            doc.setLanguage("en");
            doc.setCreatedAt(Instant.ofEpochMilli(Files.getLastModifiedTime(path).toMillis()));
            doc.setUpdatedAt(Instant.ofEpochMilli(Files.getLastModifiedTime(path).toMillis()));
            
            Map<String, Object> customFields = new HashMap<>();
            customFields.put("filePath", path.toAbsolutePath().toString());
            customFields.put("fileSize", sizeBytes);
            doc.setCustomFields(customFields);
            
            return doc;
        } catch (IOException e) {
            return null;
        }
    }

    private String inferSourceType(String parentDir) {
        String lower = parentDir.toLowerCase();
        if (lower.contains("hr") || lower.contains("human-resources")) return "HR";
        if (lower.contains("finance") || lower.contains("accounting")) return "Finance";
        if (lower.contains("wiki") || lower.contains("docs")) return "InternalWiki";
        return "FileSystem";
    }

    private String inferDepartment(String parentDir) {
        String lower = parentDir.toLowerCase();
        if (lower.contains("hr") || lower.contains("human-resources")) return "HR";
        if (lower.contains("finance")) return "Finance";
        if (lower.contains("product")) return "Product";
        if (lower.contains("sales")) return "Sales";
        if (lower.contains("engineering") || lower.contains("dev")) return "Engineering";
        return "General";
    }
}
