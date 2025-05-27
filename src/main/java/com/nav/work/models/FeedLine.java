package com.nav.work.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown fields
public class FeedLine
{
    private String id;
    private String url;
    private String title;
    private String content_text;
    private ZonedDateTime date_modified;

    @JsonProperty("_feed_entry")
    private FeedEntry _feed_entry;

    public FeedLine() {}

    public FeedEntry get_feed_entry() {
        return _feed_entry;
    }

    public void set_feed_entry(FeedEntry _feed_entry) {
        this._feed_entry = _feed_entry;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent_text() {
        return content_text;
    }

    public void setContent_text(String content_text) {
        this.content_text = content_text;
    }

    public ZonedDateTime getDate_modified() {
        return date_modified;
    }

    public void setDate_modified(ZonedDateTime date_modified) {
        this.date_modified = date_modified;
    }
}