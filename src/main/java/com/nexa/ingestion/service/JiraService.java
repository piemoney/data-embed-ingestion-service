package com.nexa.ingestion.service;

import com.nexa.ingestion.config.JiraProperties;
import com.nexa.ingestion.dto.SourceDocument;
import com.nexa.ingestion.util.HtmlToPlainText;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches issues from Jira using the Atlassian REST API.
 */
@Service
public class JiraService {

    private final WebClient webClient;
    private final JiraProperties properties;
    private final HtmlToPlainText htmlToPlainText;

    public JiraService(WebClient.Builder webClientBuilder,
                       JiraProperties properties,
                       HtmlToPlainText htmlToPlainText) {
        this.properties = properties;
        this.htmlToPlainText = htmlToPlainText;
        this.webClient = webClientBuilder
                .baseUrl(properties.getBaseUrl() + "/rest/api/3")
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String basicAuthHeader() {
        String credentials = properties.getUsername() + ":" + properties.getApiToken();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Fetches all issues from a Jira project.
     *
     * @param projectKey Jira project key (e.g., "PROJ")
     * @return flux of SourceDocument
     */
    public Flux<SourceDocument> getIssuesFromProject(String projectKey) {
        int maxIssues = properties.getMaxIssuesPerProject();
        return getIssueResults(projectKey, 0)
                .expand(result -> {
                    int startAt = result.get("startAt") != null ? ((Number) result.get("startAt")).intValue() : 0;
                    int total = result.get("total") != null ? ((Number) result.get("total")).intValue() : 0;
                    int maxResults = result.get("maxResults") != null ? ((Number) result.get("maxResults")).intValue() : 50;
                    
                    if (maxIssues > 0 && startAt + maxResults >= maxIssues) {
                        return Mono.empty();
                    }
                    if (startAt + maxResults >= total) {
                        return Mono.empty();
                    }
                    return getIssueResults(projectKey, startAt + maxResults);
                })
                .concatMap(result -> {
                    List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
                    if (issues == null) return Flux.empty();
                    return Flux.fromIterable(issues)
                            .map(this::toSourceDocument)
                            .take(maxIssues > 0 ? maxIssues : Long.MAX_VALUE);
                });
    }

    private Mono<Map<String, Object>> getIssueResults(String projectKey, int startAt) {
        String uri = UriComponentsBuilder.fromPath("/search")
                .queryParam("jql", "project=" + projectKey)
                .queryParam("startAt", startAt)
                .queryParam("maxResults", 50)
                .queryParam("fields", "summary,description,status,assignee,creator,created,updated,labels")
                .build()
                .toUriString();

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private SourceDocument toSourceDocument(Map<String, Object> issue) {
        Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
        String key = (String) issue.get("key");
        String id = (String) issue.get("id");
        
        String summary = fields != null ? (String) fields.get("summary") : "";
        Map<String, Object> description = fields != null ? (Map<String, Object>) fields.get("description") : null;
        String descriptionText = description != null ? htmlToPlainText.toPlainText((String) description.getOrDefault("content", "")) : "";
        
        String content = summary + "\n\n" + descriptionText;
        
        Map<String, Object> assignee = fields != null ? (Map<String, Object>) fields.get("assignee") : null;
        String author = assignee != null ? (String) assignee.getOrDefault("displayName", "") : "";
        
        Map<String, Object> creator = fields != null ? (Map<String, Object>) fields.get("creator") : null;
        if (author.isEmpty() && creator != null) {
            author = (String) creator.getOrDefault("displayName", "");
        }
        
        List<String> labels = fields != null ? (List<String>) fields.get("labels") : List.of();
        
        String createdStr = fields != null ? (String) fields.get("created") : null;
        Instant createdAt = parseJiraDate(createdStr);
        
        String updatedStr = fields != null ? (String) fields.get("updated") : null;
        Instant updatedAt = parseJiraDate(updatedStr);
        
        String url = properties.getBaseUrl() + "/browse/" + key;
        
        SourceDocument doc = new SourceDocument();
        doc.setId(id);
        doc.setTitle(summary);
        doc.setContent(content);
        doc.setSourceType("Jira");
        doc.setUrl(url);
        doc.setAuthor(author);
        doc.setTags(labels);
        doc.setCreatedAt(createdAt);
        doc.setUpdatedAt(updatedAt);
        doc.setSecurityLevel("internal");
        
        Map<String, Object> customFields = new HashMap<>();
        customFields.put("issueKey", key);
        Map<String, Object> status = fields != null ? (Map<String, Object>) fields.get("status") : null;
        if (status != null) {
            customFields.put("status", status.get("name"));
        }
        doc.setCustomFields(customFields);
        
        return doc;
    }

    private Instant parseJiraDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return Instant.now();
        }
        try {
            return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
