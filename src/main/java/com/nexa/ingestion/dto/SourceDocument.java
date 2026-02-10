package com.nexa.ingestion.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a document from any source (Confluence, Jira, GitHub, etc.)
 * with all metadata needed for ingestion.
 */
public class SourceDocument {
    private String id;
    private String title;
    private String content;  // Plain text content
    private String sourceType;  // Confluence, Jira, GitHub, InternalWiki, HR, Finance
    private String url;
    private String author;
    private String department;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
    private String securityLevel;  // public/internal/confidential
    private String language;  // en, hi, etc.
    private List<String> precomputedEntities;
    private Map<String, Object> customFields;

    public SourceDocument() {
    }

    public SourceDocument(String id, String title, String content, String sourceType) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.sourceType = sourceType;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public List<String> getPrecomputedEntities() { return precomputedEntities; }
    public void setPrecomputedEntities(List<String> precomputedEntities) { this.precomputedEntities = precomputedEntities; }
    public Map<String, Object> getCustomFields() { return customFields; }
    public void setCustomFields(Map<String, Object> customFields) { this.customFields = customFields; }
}
