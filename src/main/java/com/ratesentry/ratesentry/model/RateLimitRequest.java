package com.ratesentry.ratesentry.model;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class RateLimitRequest {

    @NotBlank(message = "clientId is required")
    private String clientId;

    @NotBlank(message = "endpoint is required")
    private String endpoint;

    @Min(value = 1, message = "maxRequests must be at least 1")
    private int maxRequests;

    @Min(value = 1, message = "windowSeconds must be at least 1")
    private int windowSeconds;
}