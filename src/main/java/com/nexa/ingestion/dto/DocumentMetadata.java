package com.nexa.ingestion.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Rich metadata for document chunks stored in Qdrant.
 * All fields are optional to support different source types.
 */
public class DocumentMetadata {

    private String id;  // Unique identifier for the chunk
    private String text;  // Original chunk text
    private String source;  // Document/ticket name
    private String sourceType;  // Confluence, Jira, GitHub, InternalWiki, HR, Finance, etc.
    private String url;  // Link to the document/ticket
    private String author;  // Creator or owner
    private String department;  // Owning team (Engineering, Product, HR, Finance, Sales)
    private List<String> tags;  // Keywords or labels
    private Instant createdAt;  // Creation timestamp
    private Instant updatedAt;  // Update timestamp
    private int chunkIndex;  // Position of chunk in document (0-based)
    private String embeddingModel;  // Model name/version (e.g., "BAAI/bge-m3")
    private String securityLevel;  // public/internal/confidential
    private String language;  // Language of the text (e.g., "en", "hi")
    private List<String> precomputedEntities;  // Optional important entities (e.g., "Section 80C", "PPF")
    private Map<String, Object> customFields;  // Additional source-specific fields

    public DocumentMetadata() {
    }

    public DocumentMetadata(String id, String text, String source, String sourceType) {
        this.id = id;
        this.text = text;
        this.source = source;
        this.sourceType = sourceType;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
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
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public String getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public List<String> getPrecomputedEntities() { return precomputedEntities; }
    public void setPrecomputedEntities(List<String> precomputedEntities) { this.precomputedEntities = precomputedEntities; }
    public Map<String, Object> getCustomFields() { return customFields; }
    public void setCustomFields(Map<String, Object> customFields) { this.customFields = customFields; }
}
