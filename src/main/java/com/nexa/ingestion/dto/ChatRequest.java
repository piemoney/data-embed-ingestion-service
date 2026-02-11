package com.nexa.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request DTO for chat API (RAG: embed query → Qdrant search → LLM with context).
 */
public class ChatRequest {

    @NotBlank(message = "Query cannot be blank")
    @JsonProperty("query")
    private String query;

    @PositiveOrZero
    private int limit = 5;  // Number of similar documents to retrieve from Qdrant

    @PositiveOrZero
    private double scoreThreshold = 0.0;  // Minimum similarity score (0.0 to 1.0)

    public ChatRequest() {
    }

    public ChatRequest(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public double getScoreThreshold() {
        return scoreThreshold;
    }

    public void setScoreThreshold(double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
    }
}
