package com.nexa.ingestion.dto.confluence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfluencePageResult {

    private List<ConfluencePage> results;
    private Integer size;
    private Integer limit;
    private Integer start;

    @JsonProperty("_links")
    private Links links;

    public List<ConfluencePage> getResults() { return results; }
    public void setResults(List<ConfluencePage> results) { this.results = results; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public Integer getStart() { return start; }
    public void setStart(Integer start) { this.start = start; }
    public Links getLinks() { return links; }
    public void setLinks(Links links) { this.links = links; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        private String next;
        private String prev;
        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }
        public String getPrev() { return prev; }
        public void setPrev(String prev) { this.prev = prev; }
    }
}
