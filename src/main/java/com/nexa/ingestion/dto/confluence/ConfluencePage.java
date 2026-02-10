package com.nexa.ingestion.dto.confluence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfluencePage {

    private String id;
    private String type;
    private String title;
    private String status;

    @JsonProperty("body")
    private Body body;

    @JsonProperty("_links")
    private PageLinks links;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Body getBody() { return body; }
    public void setBody(Body body) { this.body = body; }
    public PageLinks getLinks() { return links; }
    public void setLinks(PageLinks links) { this.links = links; }

    /**
     * Returns the raw HTML body, or empty string if not present.
     */
    public String getBodyHtml() {
        if (body != null && body.getStorage() != null && body.getStorage().getValue() != null) {
            return body.getStorage().getValue();
        }
        return "";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        @JsonProperty("storage")
        private Storage storage;
        public Storage getStorage() { return storage; }
        public void setStorage(Storage storage) { this.storage = storage; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Storage {
        private String value;
        private String representation;
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getRepresentation() { return representation; }
        public void setRepresentation(String representation) { this.representation = representation; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageLinks {
        private String webui;
        private String self;
        public String getWebui() { return webui; }
        public void setWebui(String webui) { this.webui = webui; }
        public String getSelf() { return self; }
        public void setSelf(String self) { this.self = self; }
    }
}
