package com.nexa.ingestion.service;

import com.nexa.ingestion.config.GitHubProperties;
import com.nexa.ingestion.dto.SourceDocument;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches files from GitHub repositories using the GitHub REST API.
 */
@Service
public class GitHubService {

    private final WebClient webClient;
    private final GitHubProperties properties;

    public GitHubService(WebClient.Builder webClientBuilder, GitHubProperties properties) {
        this.properties = properties;
        WebClient.Builder builder = webClientBuilder.baseUrl(properties.getApiUrl());
        if (properties.getApiToken() != null && !properties.getApiToken().isEmpty()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "token " + properties.getApiToken());
        }
        this.webClient = builder
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Fetches all markdown/text files from configured repositories.
     */
    public Flux<SourceDocument> getAllRepositoryFiles() {
        List<String> repos = properties.getRepositories();
        if (repos.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromIterable(repos)
                .flatMap(this::getFilesFromRepository);
    }

    /**
     * Fetches files from a specific repository.
     *
     * @param repo Repository in format "owner/repo"
     */
    public Flux<SourceDocument> getFilesFromRepository(String repo) {
        String[] parts = repo.split("/");
        if (parts.length != 2) {
            return Flux.error(new IllegalArgumentException("Invalid repository format. Use 'owner/repo'"));
        }
        String owner = parts[0];
        String repoName = parts[1];
        
        return getRepositoryTree(owner, repoName, "HEAD")
                .flatMapMany(tree -> Flux.fromIterable(tree)
                        .filter(this::shouldProcessFile)
                        .flatMap(file -> getFileContent(owner, repoName, file))
                        .filter(doc -> doc != null && !doc.getContent().isBlank()));
    }

    private Mono<List<Map<String, Object>>> getRepositoryTree(String owner, String repo, String ref) {
        String uri = String.format("/repos/%s/%s/git/trees/%s?recursive=1", owner, repo, ref);
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> tree = (List<Map<String, Object>>) response.get("tree");
                    return tree != null ? tree : List.<Map<String, Object>>of();
                })
                .onErrorResume(e -> Mono.just(List.<Map<String, Object>>of()));
    }

    private boolean shouldProcessFile(Map<String, Object> file) {
        String path = (String) file.get("path");
        String type = (String) file.get("type");
        
        if (!"blob".equals(type)) return false;
        if (path == null) return false;
        
        return properties.getFileExtensions().stream()
                .anyMatch(path::endsWith);
    }

    private Mono<SourceDocument> getFileContent(String owner, String repo, Map<String, Object> file) {
        String path = (String) file.get("path");
        String sha = (String) file.get("sha");
        Integer size = file.get("size") != null ? ((Number) file.get("size")).intValue() : 0;
        
        if (size > properties.getMaxFileSizeKb() * 1024) {
            return Mono.empty();
        }
        
        String uri = String.format("/repos/%s/%s/contents/%s", owner, repo, path);
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Map.class)
                .map(content -> {
                    String contentBase64 = (String) content.get("content");
                    String encoding = (String) content.getOrDefault("encoding", "base64");
                    
                    String text;
                    if ("base64".equals(encoding) && contentBase64 != null) {
                        text = new String(Base64.getDecoder().decode(contentBase64.replaceAll("\\s", "")), StandardCharsets.UTF_8);
                    } else {
                        text = contentBase64 != null ? contentBase64 : "";
                    }
                    
                    String name = (String) content.getOrDefault("name", path);
                    String url = (String) content.getOrDefault("html_url", "");
                    String author = "";
                    Map<String, Object> authorObj = (Map<String, Object>) content.get("author");
                    if (authorObj != null) {
                        author = (String) authorObj.getOrDefault("login", "");
                    }
                    
                    SourceDocument doc = new SourceDocument();
                    doc.setId(sha);
                    doc.setTitle(name);
                    doc.setContent(text);
                    doc.setSourceType("GitHub");
                    doc.setUrl(url);
                    doc.setAuthor(author);
                    doc.setDepartment("Engineering");
                    doc.setSecurityLevel("public");
                    doc.setLanguage("en");
                    
                    Map<String, Object> customFields = new HashMap<>();
                    customFields.put("repository", repo);
                    customFields.put("path", path);
                    doc.setCustomFields(customFields);
                    
                    return doc;
                })
                .onErrorResume(e -> Mono.empty());
    }
}
