package com.nexa.ingestion.service;

import com.nexa.ingestion.config.ConfluenceProperties;
import com.nexa.ingestion.dto.confluence.ConfluencePage;
import com.nexa.ingestion.dto.confluence.ConfluencePageResult;
import com.nexa.ingestion.util.HtmlToPlainText;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Fetches content from Confluence using the Atlassian REST API.
 * Uses WebClient (reactive) for all HTTP calls.
 */
@Service
public class ConfluenceService {

    private final WebClient webClient;
    private final ConfluenceProperties properties;
    private final HtmlToPlainText htmlToPlainText;

    public ConfluenceService(WebClient.Builder webClientBuilder,
                             ConfluenceProperties properties,
                             HtmlToPlainText htmlToPlainText) {
        this.properties = properties;
        this.htmlToPlainText = htmlToPlainText;
        this.webClient = webClientBuilder
                .baseUrl(properties.getContentUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String basicAuthHeader() {
        String credentials = properties.getUsername() + ":" + properties.getApiToken();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Fetches all pages in a Confluence space using CQL.
     * Supports pagination and optional limit via configuration.
     *
     * @param spaceKey Confluence space key (e.g. "TEAM", "DOC")
     * @return flux of ConfluencePage with body.storage expanded
     */
    public Flux<ConfluencePage> getPagesInSpace(String spaceKey) {
        int maxPages = properties.getMaxPagesPerSpace();
        return getPageResults(spaceKey, 0, null)
                .expand(result -> {
                    if (result.getResults() == null || result.getResults().isEmpty()) {
                        return Mono.empty();
                    }
                    int start = result.getStart() != null ? result.getStart() : 0;
                    int size = result.getSize() != null ? result.getSize() : 0;
                    int limit = result.getLimit() != null ? result.getLimit() : 25;
                    if (maxPages > 0 && start + size >= maxPages) {
                        return Mono.empty();
                    }
                    String next = result.getLinks() != null ? result.getLinks().getNext() : null;
                    if (next == null || next.isBlank()) {
                        return Mono.empty();
                    }
                    return getNextPage(next);
                })
                .concatMap(result -> Flux.fromIterable(result.getResults() != null ? result.getResults() : List.of()))
                .take(maxPages > 0 ? maxPages : Long.MAX_VALUE);
    }

    /**
     * Fetches one page of results for the given space.
     * CQL: type=page AND space=key, expand=body.storage
     */
    public Mono<ConfluencePageResult> getPageResults(String spaceKey, int start, Integer limit) {
        int pageSize = limit != null ? limit : 25;
        String uri = UriComponentsBuilder.fromPath("")
                .queryParam("cql", "type=page AND space=" + spaceKey)
                .queryParam("expand", "body.storage")
                .queryParam("start", start)
                .queryParam("limit", pageSize)
                .build()
                .toUriString();

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(ConfluencePageResult.class);
    }

    private Mono<ConfluencePageResult> getNextPage(String nextUrl) {
        String fullUrl = nextUrl.startsWith("http") ? nextUrl : properties.getBaseUrl() + nextUrl;
        return WebClient.create()
                .get()
                .uri(fullUrl)
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(ConfluencePageResult.class);
    }

    /**
     * Extracts page title and body as plain text from a Confluence page.
     *
     * @param page Confluence page with body.storage
     * @return [title, plainTextBody] as a simple record or pair; we use title + " " + body for full text
     */
    public String getPageTitleAndPlainText(ConfluencePage page) {
        String title = page.getTitle() != null ? page.getTitle() : "";
        String bodyHtml = page.getBodyHtml();
        String plainBody = htmlToPlainText.toPlainText(bodyHtml);
        if (plainBody.isEmpty()) {
            return title;
        }
        return title + "\n\n" + plainBody;
    }
}
