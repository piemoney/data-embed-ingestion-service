package com.nexa.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for chat API.
 */
public class ChatRequest {

    @NotBlank(message = "Query cannot be blank")
    @JsonProperty("query")
    private String query;

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
}
