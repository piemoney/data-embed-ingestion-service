package com.nexa.ingestion.dto;

/**
 * A segment of a document (e.g. a Confluence page) prepared for embedding.
 */
public class DocumentChunk {

    private String chunkId;
    private String pageId;
    private String pageTitle;
    private int chunkIndex;
    private String text;

    public DocumentChunk() {
    }

    public DocumentChunk(String chunkId, String pageId, String pageTitle, int chunkIndex, String text) {
        this.chunkId = chunkId;
        this.pageId = pageId;
        this.pageTitle = pageTitle;
        this.chunkIndex = chunkIndex;
        this.text = text;
    }

    public static DocumentChunkBuilder builder() {
        return new DocumentChunkBuilder();
    }

    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }
    public String getPageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }
    public String getPageTitle() { return pageTitle; }
    public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public static final class DocumentChunkBuilder {
        private String chunkId;
        private String pageId;
        private String pageTitle;
        private int chunkIndex;
        private String text;

        public DocumentChunkBuilder chunkId(String chunkId) { this.chunkId = chunkId; return this; }
        public DocumentChunkBuilder pageId(String pageId) { this.pageId = pageId; return this; }
        public DocumentChunkBuilder pageTitle(String pageTitle) { this.pageTitle = pageTitle; return this; }
        public DocumentChunkBuilder chunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public DocumentChunkBuilder text(String text) { this.text = text; return this; }
        public DocumentChunk build() { return new DocumentChunk(chunkId, pageId, pageTitle, chunkIndex, text); }
    }
}
