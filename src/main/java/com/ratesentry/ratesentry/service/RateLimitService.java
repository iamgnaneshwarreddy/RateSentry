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
    public RateLimitResponse checkTokenBucket(RateLimitRequest request) {
        String key = "tokenbucket:" + request.getClientId() + ":" + request.getEndpoint();

        String tokensKey = key + ":tokens";
        String lastRefillKey = key + ":lastRefill";

        long now = Instant.now().getEpochSecond();

        int maxTokens = request.getMaxRequests();
        int refillRate = request.getMaxRequests(); // tokens per window
        int windowSeconds = request.getWindowSeconds();

        // Fetch current state
        String tokensStr = redisTemplate.opsForValue().get(tokensKey);
        String lastRefillStr = redisTemplate.opsForValue().get(lastRefillKey);

        double currentTokens = tokensStr != null ? Double.parseDouble(tokensStr) : maxTokens;
        long lastRefillTime = lastRefillStr != null ? Long.parseLong(lastRefillStr) : now;

        // Calculate tokens to add
        double tokensToAdd = ((now - lastRefillTime) * refillRate) / (double) windowSeconds;
        currentTokens = Math.min(maxTokens, currentTokens + tokensToAdd);

        RateLimitResponse response = new RateLimitResponse();

        if (currentTokens >= 1) {
            // Allow request
            currentTokens -= 1;

            redisTemplate.opsForValue().set(tokensKey, String.valueOf(currentTokens));
            redisTemplate.opsForValue().set(lastRefillKey, String.valueOf(now));

            redisTemplate.expire(tokensKey, windowSeconds, TimeUnit.SECONDS);
            redisTemplate.expire(lastRefillKey, windowSeconds, TimeUnit.SECONDS);

            response.setAllowed(true);
            response.setRemainingRequests((int) currentTokens);
            response.setResetTimeSeconds(now + windowSeconds);
            response.setMessage("Request allowed (Token Bucket)");
        } else {
            // Deny request
            redisTemplate.opsForValue().set(tokensKey, String.valueOf(currentTokens));
            redisTemplate.opsForValue().set(lastRefillKey, String.valueOf(now));

            response.setAllowed(false);
            response.setRemainingRequests(0);
            response.setResetTimeSeconds(now + windowSeconds);
            response.setMessage("Rate limit exceeded (Token Bucket)");
        }

        return response;
    }
}