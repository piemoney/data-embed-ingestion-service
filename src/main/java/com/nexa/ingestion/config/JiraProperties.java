package com.nexa.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@ConfigurationProperties(prefix = "jira")
@Validated
public class JiraProperties {

    @NotBlank
    private String baseUrl = "https://your-instance.atlassian.net";

    private String username = "";
    private String apiToken = "";

    @PositiveOrZero
    private int maxIssuesPerProject = 0;  // 0 = no limit

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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

    public int getMaxIssuesPerProject() {
        return maxIssuesPerProject;
    }

    public void setMaxIssuesPerProject(int maxIssuesPerProject) {
        this.maxIssuesPerProject = maxIssuesPerProject;
    }
}
