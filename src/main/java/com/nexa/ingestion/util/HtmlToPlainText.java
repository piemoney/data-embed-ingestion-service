package com.nexa.ingestion.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Converts HTML to plain text for embedding (strips tags, normalizes whitespace).
 */
@Component
public class HtmlToPlainText {

    /**
     * Converts HTML to plain text: strips all tags and normalizes whitespace.
     *
     * @param html raw HTML (e.g. Confluence storage format)
     * @return plain text, never null
     */
    public String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String cleaned = Jsoup.clean(html, Safelist.none());
        String text = Jsoup.parse(cleaned).text();
        return normalizeWhitespace(text);
    }

    private static String normalizeWhitespace(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }
}
