package com.ratesentry.ratesentry.model;

import lombok.Data;

@Data
public class RateLimitResponse {
    private boolean allowed;          // is request allowed?
    private int remainingRequests;    // how many left
    private long resetTimeSeconds;    // when does window reset
    private String message;           // human readable message
}