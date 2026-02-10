package com.nexa.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request for semantic search with LLM response.
 */
public class SearchRequest {

    @NotBlank(message = "Query cannot be blank")
    private String query;

    @PositiveOrZero
    private int limit = 5;  // Number of similar documents to retrieve

    @PositiveOrZero
    private double scoreThreshold = 0.0;  // Minimum similarity score (0.0 to 1.0)

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
