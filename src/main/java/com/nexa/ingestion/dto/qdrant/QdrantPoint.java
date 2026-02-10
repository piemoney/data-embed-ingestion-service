package com.nexa.ingestion.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QdrantPoint {

    private String id;
    private float[] vector;
    private Map<String, Object> payload;

    public QdrantPoint() {
    }

    public QdrantPoint(String id, float[] vector, Map<String, Object> payload) {
        this.id = id;
        this.vector = vector;
        this.payload = payload;
    }

    public static QdrantPointBuilder builder() {
        return new QdrantPointBuilder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public float[] getVector() { return vector; }
    public void setVector(float[] vector) { this.vector = vector; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public static final class QdrantPointBuilder {
        private String id;
        private float[] vector;
        private Map<String, Object> payload;

        public QdrantPointBuilder id(String id) { this.id = id; return this; }
        public QdrantPointBuilder vector(float[] vector) { this.vector = vector; return this; }
        public QdrantPointBuilder payload(Map<String, Object> payload) { this.payload = payload; return this; }
        public QdrantPoint build() { return new QdrantPoint(id, vector, payload); }
    }
}
