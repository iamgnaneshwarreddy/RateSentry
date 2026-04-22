package com.ratesentry.ratesentry.model;

import lombok.Data;

@Data
public class RateLimitRequest {
    private String clientId;      // who is making the request
    private String endpoint;      // which API endpoint
    private int maxRequests;      // max allowed requests
    private int windowSeconds;    // time window in seconds
}