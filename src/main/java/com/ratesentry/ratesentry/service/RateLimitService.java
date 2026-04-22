package com.ratesentry.ratesentry.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.ratesentry.ratesentry.model.RateLimitRequest;
import com.ratesentry.ratesentry.model.RateLimitResponse;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public RateLimitResponse checkRateLimit(RateLimitRequest request) {
        String key = "ratelimit:" + request.getClientId() + ":" + request.getEndpoint();
        long now = Instant.now().getEpochSecond();
        long windowStart = now - request.getWindowSeconds();

        // Remove requests outside the window
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Count requests in current window
        Long count = redisTemplate.opsForZSet().zCard(key);
        count = count == null ? 0 : count;

        RateLimitResponse response = new RateLimitResponse();

        if (count < request.getMaxRequests()) {
            // Allow request — add current timestamp
            redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
            redisTemplate.expire(key, request.getWindowSeconds(), TimeUnit.SECONDS);

            response.setAllowed(true);
            response.setRemainingRequests((int)(request.getMaxRequests() - count - 1));
            response.setResetTimeSeconds(now + request.getWindowSeconds());
            response.setMessage("Request allowed");
        } else {
            // Deny request
            response.setAllowed(false);
            response.setRemainingRequests(0);
            response.setResetTimeSeconds(now + request.getWindowSeconds());
            response.setMessage("Rate limit exceeded. Try again later.");
        }

        return response;
    }
}