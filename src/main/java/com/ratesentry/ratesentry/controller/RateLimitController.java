package com.ratesentry.ratesentry.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ratesentry.ratesentry.model.RateLimitRequest;
import com.ratesentry.ratesentry.model.RateLimitResponse;
import com.ratesentry.ratesentry.service.RateLimitService;

@RestController
@RequestMapping("/api/ratelimit")
public class RateLimitController {

    @Autowired
    private RateLimitService rateLimitService;

    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> checkRateLimit(@RequestBody RateLimitRequest request) {
        RateLimitResponse response = rateLimitService.checkRateLimit(request);

        if(response.isAllowed()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(429).body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RateSentry is running");
    }
}