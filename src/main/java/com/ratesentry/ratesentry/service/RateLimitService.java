package com.ratesentry.ratesentry.service;

import com.ratesentry.ratesentry.model.RateLimitLog;
import com.ratesentry.ratesentry.repository.ClientConfigRepository;
import com.ratesentry.ratesentry.repository.RateLimitLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.script.RedisScript;

import com.ratesentry.ratesentry.model.RateLimitRequest;
import com.ratesentry.ratesentry.model.RateLimitResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RateLimitLogRepository logRepository;

    @Autowired
    private ClientConfigRepository clientConfigRepository;

    private static final String SLIDING_WINDOW_SCRIPT =
            "local key = KEYS[1] " +
                    "local now = tonumber(ARGV[1]) " +
                    "local window = tonumber(ARGV[2]) " +
                    "local max = tonumber(ARGV[3]) " +
                    "local windowStart = now - window " +
                    "redis.call('ZREMRANGEBYSCORE', key, 0, windowStart) " +
                    "local count = redis.call('ZCARD', key) " +
                    "if count < max then " +
                    "  redis.call('ZADD', key, now, now) " +
                    "  redis.call('EXPIRE', key, window) " +
                    "  return 1 " +
                    "else " +
                    "  return 0 " +
                    "end";


    public RateLimitResponse checkRateLimit(RateLimitRequest request) {
        request = enrichWithConfig(request);

        String key = "ratelimit:" + request.getClientId() + ":" + request.getEndpoint();
        long now = Instant.now().getEpochSecond();

        RedisScript<Long> script = RedisScript.of(SLIDING_WINDOW_SCRIPT, Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(key),
                String.valueOf(now),
                String.valueOf(request.getWindowSeconds()),
                String.valueOf(request.getMaxRequests())
        );

        boolean allowed = result != null && result == 1L;

        RateLimitResponse response = new RateLimitResponse();

        if (allowed) {
            response.setAllowed(true);
            response.setRemainingRequests(request.getMaxRequests() - 1);
            response.setResetTimeSeconds(now + request.getWindowSeconds());
            response.setMessage("Request allowed");
        } else {
            response.setAllowed(false);
            response.setRemainingRequests(0);
            response.setResetTimeSeconds(now + request.getWindowSeconds());
            response.setMessage("Rate limit exceeded. Try again later.");
        }

        logRequest(request, response, "SLIDING_WINDOW");
        return response;
    }
    public RateLimitResponse checkTokenBucket(RateLimitRequest request) {
        String key = "tokenbucket:" + request.getClientId() + ":" + request.getEndpoint();

        String tokensKey = key + ":tokens";
        String lastRefillKey = key + ":lastRefill";

        long now = Instant.now().getEpochSecond();

        request = enrichWithConfig(request);

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
        logRequest(request, response, "TOKEN_BUCKET");
        return response;
    }
    private void logRequest(RateLimitRequest request, RateLimitResponse response, String algorithm) {
        RateLimitLog log = new RateLimitLog();
        log.setClientId(request.getClientId());
        log.setEndpoint(request.getEndpoint());
        log.setAllowed(response.isAllowed());
        log.setAlgorithm(algorithm);
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
    }

    private RateLimitRequest enrichWithConfig(RateLimitRequest request) {
        clientConfigRepository.findById(request.getClientId()).ifPresent(config -> {
            request.setMaxRequests(config.getMaxRequests());
            request.setWindowSeconds(config.getWindowSeconds());
        });
        return request;
    }

    public List<RateLimitLog> getClientLogs(String clientId) {
        return logRepository.findByClientId(clientId);
    }

    public List<RateLimitLog> getViolations() {
        return logRepository.findByAllowedFalse();
    }
}