package com.nexa.ingestion.dto;

import java.util.List;

/**
 * Response from search API with retrieved context and LLM-generated answer.
 */
public class SearchResponse {

    private String query;
    private List<SearchResult> results;  // Retrieved similar documents
    private String answer;  // LLM-generated answer
    private String model;  // LLM model used

    public SearchResponse() {
    }

    public SearchResponse(String query, List<SearchResult> results, String answer, String model) {
        this.query = query;
        this.results = results;
        this.answer = answer;
        this.model = model;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<SearchResult> getResults() {
        return results;
    }

    public void setResults(List<SearchResult> results) {
        this.results = results;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
