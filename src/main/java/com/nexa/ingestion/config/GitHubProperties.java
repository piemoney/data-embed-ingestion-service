package com.nexa.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@ConfigurationProperties(prefix = "github")
@Validated
public class GitHubProperties {

    @NotBlank
    private String apiUrl = "https://api.github.com";

    private String apiToken = "";  // GitHub Personal Access Token

    private String repositories = "";  // Comma-separated: "owner/repo1,owner/repo2"

    private List<String> fileExtensions = List.of(".md", ".txt", ".rst", ".adoc");  // Files to ingest

    private int maxFileSizeKb = 500;  // Skip files larger than this

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public List<String> getRepositories() {
        if (repositories == null || repositories.isBlank()) {
            return List.of();
        }
        return List.of(repositories.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public void setRepositories(String repositories) {
        this.repositories = repositories;
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
}
