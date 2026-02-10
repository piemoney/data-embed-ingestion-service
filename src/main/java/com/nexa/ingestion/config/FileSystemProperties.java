package com.nexa.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties(prefix = "filesystem")
@Validated
public class FileSystemProperties {

    private String basePaths = "";  // Comma-separated directories to scan

    private List<String> fileExtensions = List.of(".md", ".txt", ".pdf", ".docx", ".html");

    private int maxFileSizeKb = 1000;  // Skip files larger than this

    private boolean recursive = true;  // Recursively scan subdirectories

    public List<String> getBasePaths() {
        if (basePaths == null || basePaths.isBlank()) {
            return List.of();
        }
        return List.of(basePaths.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public void setBasePaths(String basePaths) {
        this.basePaths = basePaths;
    }

    public List<String> getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(List<String> fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public int getMaxFileSizeKb() {
        return maxFileSizeKb;
    }

    public void setMaxFileSizeKb(int maxFileSizeKb) {
        this.maxFileSizeKb = maxFileSizeKb;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }
}
