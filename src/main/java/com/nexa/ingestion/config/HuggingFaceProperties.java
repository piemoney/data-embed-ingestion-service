package com.nexa.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "huggingface")
@Validated
public class HuggingFaceProperties {

    @NotBlank
    private String apiUrl = "https://api-inference.huggingface.co";

    /**
     * API endpoint path (e.g., "/v1/embeddings", "/api/predict", "/run").
     * Default: "/v1/embeddings" for OpenAI-compatible Spaces, "/api/predict" for Gradio Spaces.
     */
    private String endpoint = "/v1/embeddings";

    @NotBlank
    private String model = "BAAI/bge-base-en-v1.5";

    private String apiToken = "";

    @Positive
    private int maxTokensPerRequest = 512;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint != null ? endpoint : "/v1/embeddings";
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public int getMaxTokensPerRequest() {
        return maxTokensPerRequest;
    }

    public void setMaxTokensPerRequest(int maxTokensPerRequest) {
        this.maxTokensPerRequest = maxTokensPerRequest;
    }

    public String getInferenceUrl() {
        return apiUrl + "/pipeline/feature-extraction/" + model;
    }
}
