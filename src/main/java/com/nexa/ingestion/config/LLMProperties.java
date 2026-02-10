package com.nexa.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "llm")
@Validated
public class LLMProperties {

    /**
     * Hugging Face OpenAI-compatible API base URL.
     * Default: https://router.huggingface.co/v1
     */
    @NotBlank
    private String apiUrl = "https://router.huggingface.co/v1";

    /**
     * LLM model name (e.g., "meta-llama/Llama-3.1-8B-Instruct:novita", "meta-llama/Llama-3.1-8B-Instruct").
     * Format: "username/model-name" or "username/model-name:provider" for specific providers.
     */
    @NotBlank
    private String model = "meta-llama/Llama-3.1-8B-Instruct:novita";

    /**
     * Hugging Face API token (required).
     */
    private String apiToken = "";

    /**
     * Maximum tokens in the generated response.
     */
    @Positive
    private int maxTokens = 512;

    /**
     * Temperature for generation (0.0 to 1.0). Higher = more creative.
     */
    private double temperature = 0.7;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
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

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
