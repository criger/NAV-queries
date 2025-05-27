package com.nav.work.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Feed
{
    private String id;
    private String version;
    private String title;
    private String home_page_url;
    private String feed_url;
    private String description;
    private List<FeedLine> items = new ArrayList<>(); //

    // De følgende to variablene får @JsonProperty for å matche mot riktig felt i json objektet
    @JsonProperty("next_url")
    private String nextUrl;
    @JsonProperty("next_id")
    private String nextId;

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

    public void setItems(List<FeedLine> items) {
        this.items = items;
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

    public List<FeedLine> getItems() {
        return items;
    }
}