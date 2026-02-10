package com.nexa.ingestion.service;

import com.nexa.ingestion.config.IngestionProperties;
import com.nexa.ingestion.dto.DocumentChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits document text into chunks of approximately 500–800 tokens.
 * Uses character-based estimation when a tokenizer is not available.
 */
@Service
public class ChunkingService {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile(
            "(?<=[.!?])\\s+|(?<=[.!?])$"
    );

    private final IngestionProperties ingestionProperties;

    public ChunkingService(IngestionProperties ingestionProperties) {
        this.ingestionProperties = ingestionProperties;
    }

    /**
     * Splits plain text into chunks with target size in tokens (estimated by chars / charsPerToken).
     * Tries to break at sentence boundaries when possible.
     *
     * @param pageId   source page ID
     * @param pageTitle source page title
     * @param plainText full plain text of the page
     * @return list of document chunks
     */
    public List<DocumentChunk> chunk(String pageId, String pageTitle, String plainText) {
        List<DocumentChunk> chunks = new ArrayList<>();
        if (plainText == null || plainText.isBlank()) {
            return chunks;
        }

        int charsPerToken = ingestionProperties.getCharsPerToken();
        int minChars = ingestionProperties.getChunk().getTargetTokensMin() * charsPerToken;
        int maxChars = ingestionProperties.getChunk().getTargetTokensMax() * charsPerToken;

        String normalized = plainText.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxChars) {
            chunks.add(DocumentChunk.builder()
                    .chunkId(chunkId(pageId, 0))
                    .pageId(pageId)
                    .pageTitle(pageTitle)
                    .chunkIndex(0)
                    .text(normalized)
                    .build());
            return chunks;
        }

        List<String> segments = splitAtSentences(normalized, minChars, maxChars);
        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i).trim();
            if (segment.isEmpty()) continue;
            chunks.add(DocumentChunk.builder()
                    .chunkId(chunkId(pageId, i))
                    .pageId(pageId)
                    .pageTitle(pageTitle)
                    .chunkIndex(i)
                    .text(segment)
                    .build());
        }
        return chunks;
    }

    /**
     * Splits text into segments of roughly minChars–maxChars, breaking at sentence boundaries when possible.
     */
    private List<String> splitAtSentences(String text, int minChars, int maxChars) {
        List<String> result = new ArrayList<>();
        String[] sentences = SENTENCE_BOUNDARY.split(text);
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            String s = sentence.trim();
            if (s.isEmpty()) continue;

            if (current.length() + s.length() + 1 <= maxChars) {
                if (current.length() > 0) current.append(" ");
                current.append(s);
            } else {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current = new StringBuilder();
                }
                if (s.length() > maxChars) {
                    for (int i = 0; i < s.length(); i += maxChars) {
                        int end = Math.min(i + maxChars, s.length());
                        result.add(s.substring(i, end));
                    }
                } else {
                    current.append(s);
                }
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private static String chunkId(String pageId, int index) {
        return pageId + "_chunk_" + index;
    }
}
