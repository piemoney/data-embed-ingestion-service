package com.nexa.ingestion.service;

import com.nexa.ingestion.config.IngestionProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Semantic chunking with overlap support.
 * Splits text into chunks of ~300-500 tokens with ~50 token overlap.
 */
@Service
public class SemanticChunkingService {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile(
            "(?<=[.!?])\\s+|(?<=[.!?])$"
    );
    private static final Pattern PARAGRAPH_BOUNDARY = Pattern.compile("\\n\\s*\\n");

    private final IngestionProperties ingestionProperties;

    public SemanticChunkingService(IngestionProperties ingestionProperties) {
        this.ingestionProperties = ingestionProperties;
    }

    /**
     * Splits text into semantic chunks with overlap.
     *
     * @param text full text to chunk
     * @param targetTokensMin minimum tokens per chunk (default: 300)
     * @param targetTokensMax maximum tokens per chunk (default: 500)
     * @param overlapTokens overlap between chunks (default: 50)
     * @return list of text chunks
     */
    public List<String> chunkWithOverlap(String text, int targetTokensMin, int targetTokensMax, int overlapTokens) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int charsPerToken = ingestionProperties.getCharsPerToken();
        int minChars = targetTokensMin * charsPerToken;
        int maxChars = targetTokensMax * charsPerToken;
        int overlapChars = overlapTokens * charsPerToken;

        String normalized = text.replaceAll("\\s+", " ").trim();
        
        // Try to split by paragraphs first (better semantic boundaries)
        String[] paragraphs = PARAGRAPH_BOUNDARY.split(normalized);
        List<String> segments = new ArrayList<>();
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.length() <= maxChars) {
                segments.add(trimmed);
            } else {
                // Paragraph too long, split by sentences
                segments.addAll(splitBySentences(trimmed, minChars, maxChars));
            }
        }

        // Build chunks with overlap
        int currentPos = 0;
        while (currentPos < segments.size()) {
            StringBuilder chunkBuilder = new StringBuilder();
            int chunkStart = currentPos;
            int chunkCharCount = 0;

            // Build chunk up to maxChars
            while (currentPos < segments.size() && chunkCharCount < maxChars) {
                String segment = segments.get(currentPos);
                if (chunkCharCount + segment.length() + 1 <= maxChars) {
                    if (chunkBuilder.length() > 0) chunkBuilder.append(" ");
                    chunkBuilder.append(segment);
                    chunkCharCount += segment.length() + 1;
                    currentPos++;
                } else {
                    break;
                }
            }

            String chunk = chunkBuilder.toString().trim();
            if (!chunk.isEmpty() && chunk.length() >= minChars) {
                chunks.add(chunk);
            }

            // Move back by overlap amount for next chunk
            if (currentPos < segments.size()) {
                int overlapSegments = Math.max(1, (overlapChars / (maxChars / Math.max(1, currentPos - chunkStart))));
                currentPos = Math.max(chunkStart + 1, currentPos - overlapSegments);
            }
        }

        return chunks;
    }

    /**
     * Splits text into chunks using default settings (300-500 tokens, 50 overlap).
     */
    public List<String> chunkWithOverlap(String text) {
        return chunkWithOverlap(text, 300, 500, 50);
    }

    private List<String> splitBySentences(String text, int minChars, int maxChars) {
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
                if (current.length() >= minChars) {
                    result.add(current.toString());
                }
                current = new StringBuilder(s);
            }
        }
        if (current.length() >= minChars) {
            result.add(current.toString());
        }
        return result;
    }
}
