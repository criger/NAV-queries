package com.nav.work.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty; // For next_url to nextUrl
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown fields to prevent errors
public class Feed {
    private String version;
    private String title;
    private String home_page_url;
    private String feed_url;
    private String description;
    @JsonProperty("next_url") // Map JSON next_url to nextUrl Java field
    private String nextUrl;
    private String id;
    @JsonProperty("next_id") // Map JSON next_id to nextId Java field
    private String nextId;
    private List<FeedLine> feedLines; //

    public void setVersion(String version) {
        this.version = version;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setHome_page_url(String home_page_url) {
        this.home_page_url = home_page_url;
    }

    public void setFeed_url(String feed_url) {
        this.feed_url = feed_url;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setNextUrl(String nextUrl) {
        this.nextUrl = nextUrl;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNextId(String nextId) {
        this.nextId = nextId;
    }

    public void setFeedLines(List<FeedLine> feedLines) {
        this.feedLines = feedLines;
    }

    public String getVersion() {
        return version;
    }

    public String getTitle() {
        return title;
    }

    public String getHome_page_url() {
        return home_page_url;
    }

    public String getFeed_url() {
        return feed_url;
    }

    public String getDescription() {
        return description;
    }

    public String getNextUrl() {
        return nextUrl;
    }

    public String getId() {
        return id;
    }

    public String getNextId() {
        return nextId;
    }

    public List<FeedLine> getFeedLines() {
        return feedLines;
    }
}