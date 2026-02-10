package com.nexa.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "qdrant")
@Validated
public class QdrantProperties {

    /**
     * Qdrant host (without protocol, e.g. "localhost" or "xxx.us-east-1-1.aws.cloud.qdrant.io")
     */
    @NotBlank
    private String host = "localhost";

    /**
     * Qdrant gRPC port (default 6334 for Cloud, 6334 for local)
     */
    @Positive
    private int port = 6334;

    /**
     * Use TLS (true for Cloud, false for local)
     */
    private boolean useTls = false;

    @NotBlank
    private String collectionName = "knowledge-base";

    @Positive
    private int vectorSize = 768;

    /**
     * API key for Qdrant Cloud or secured instances. Leave empty for local unauthenticated Qdrant.
     */
    private String apiKey = "";

    private boolean recreateCollection = false;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public int getVectorSize() {
        return vectorSize;
    }

    public void setVectorSize(int vectorSize) {
        this.vectorSize = vectorSize;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey : "";
    }

    public boolean isRecreateCollection() {
        return recreateCollection;
    }

    public void setRecreateCollection(boolean recreateCollection) {
        this.recreateCollection = recreateCollection;
    }
}
