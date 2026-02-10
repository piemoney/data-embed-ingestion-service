package com.nexa.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "ingestion")
@Validated
public class IngestionProperties {

    private Chunk chunk = new Chunk();

    /**
     * Approximate characters per token for English (used when no tokenizer is available).
     */
    @Positive
    private int charsPerToken = 4;

    public Chunk getChunk() {
        return chunk;
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public int getCharsPerToken() {
        return charsPerToken;
    }

    public void setCharsPerToken(int charsPerToken) {
        this.charsPerToken = charsPerToken;
    }

    @Positive
    private int batchSize = 50;

    @Positive
    private int embedBatchSize = 8;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getEmbedBatchSize() {
        return embedBatchSize;
    }

    public void setEmbedBatchSize(int embedBatchSize) {
        this.embedBatchSize = embedBatchSize;
    }

    public static class Chunk {
        @Positive
        private int targetTokensMin = 300;

        @Positive
        private int targetTokensMax = 500;

        @Positive
        private int overlapTokens = 50;

        public int getTargetTokensMin() {
            return targetTokensMin;
        }

        public void setTargetTokensMin(int targetTokensMin) {
            this.targetTokensMin = targetTokensMin;
        }

        public int getTargetTokensMax() {
            return targetTokensMax;
        }

        public void setTargetTokensMax(int targetTokensMax) {
            this.targetTokensMax = targetTokensMax;
        }

        public int getOverlapTokens() {
            return overlapTokens;
        }

        public void setOverlapTokens(int overlapTokens) {
            this.overlapTokens = overlapTokens;
        }
    }
}
