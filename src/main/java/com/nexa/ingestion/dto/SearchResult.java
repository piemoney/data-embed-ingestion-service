package com.nexa.ingestion.dto;

/**
 * A single search result from vector database.
 */
public class SearchResult {

    private String chunkId;
    private String pageId;
    private String pageTitle;
    private int chunkIndex;
    private String text;
    private double score;  // Similarity score (0.0 to 1.0)

    public SearchResult() {
    }

    public SearchResult(String chunkId, String pageId, String pageTitle, int chunkIndex, String text, double score) {
        this.chunkId = chunkId;
        this.pageId = pageId;
        this.pageTitle = pageTitle;
        this.chunkIndex = chunkIndex;
        this.text = text;
        this.score = score;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
