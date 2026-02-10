package com.nexa.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@ConfigurationProperties(prefix = "confluence")
@Validated
public class ConfluenceProperties {

    @NotBlank
    private String baseUrl = "https://your-instance.atlassian.net/wiki";

    private String apiPath = "/wiki/rest/api/content";

    private String username = "";

    private String apiToken = "";

    /**
     * Maximum pages to fetch per space. 0 means no limit.
     */
    @PositiveOrZero
    private int maxPagesPerSpace = 0;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public int getMaxPagesPerSpace() {
        return maxPagesPerSpace;
    }

    public void setMaxPagesPerSpace(int maxPagesPerSpace) {
        this.maxPagesPerSpace = maxPagesPerSpace;
    }

    public String getContentUrl() {
        return baseUrl + apiPath;
    }
}
