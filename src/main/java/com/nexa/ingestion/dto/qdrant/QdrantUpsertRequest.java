package com.nexa.ingestion.dto.qdrant;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QdrantUpsertRequest {

    private List<QdrantPoint> points;
    private Boolean wait;

    public QdrantUpsertRequest() {
    }

    public QdrantUpsertRequest(List<QdrantPoint> points, Boolean wait) {
        this.points = points;
        this.wait = wait;
    }

    public static QdrantUpsertRequestBuilder builder() {
        return new QdrantUpsertRequestBuilder();
    }

    public List<QdrantPoint> getPoints() { return points; }
    public void setPoints(List<QdrantPoint> points) { this.points = points; }
    public Boolean getWait() { return wait; }
    public void setWait(Boolean wait) { this.wait = wait; }

    public static final class QdrantUpsertRequestBuilder {
        private List<QdrantPoint> points;
        private Boolean wait;

        public QdrantUpsertRequestBuilder points(List<QdrantPoint> points) { this.points = points; return this; }
        public QdrantUpsertRequestBuilder wait(Boolean wait) { this.wait = wait; return this; }
        public QdrantUpsertRequest build() { return new QdrantUpsertRequest(points, wait); }
    }
}
