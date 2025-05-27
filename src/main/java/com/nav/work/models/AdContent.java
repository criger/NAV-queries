package com.nav.work.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdContent
{

    // klassen er ikke lenger i bruk da det tok altfor lang tid å hente en stk annonse (ca 100 ms).
    // når dette ble multiplisert med noen hundre annonser, om ikke tusener, tar jobben altfor lang tid
    // resultatet ble at man fikk en HTTP TimeOutException, selv med høy timeout limit i HttpRequest

    private String description;
    private String title;

    public AdContent() {}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}