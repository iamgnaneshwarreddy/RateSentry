package com.ratesentry.ratesentry;

import com.ratesentry.ratesentry.model.ClientConfig;
import com.ratesentry.ratesentry.model.RateLimitLog;
import com.ratesentry.ratesentry.model.RateLimitRequest;
import com.ratesentry.ratesentry.model.RateLimitResponse;
import com.ratesentry.ratesentry.repository.ClientConfigRepository;
import com.ratesentry.ratesentry.repository.RateLimitLogRepository;
import com.ratesentry.ratesentry.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RateLimitLogRepository logRepository;

    @Mock
    private ClientConfigRepository clientConfigRepository;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private RateLimitService rateLimitService;

    private RateLimitRequest request;

    @BeforeEach
    void setUp() {
        request = new RateLimitRequest();
        request.setClientId("user123");
        request.setEndpoint("/api/products");
        request.setMaxRequests(5);
        request.setWindowSeconds(60);

        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(clientConfigRepository.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    void checkRateLimit_shouldAllowRequest_whenUnderLimit() {
        when(zSetOperations.removeRangeByScore(any(), anyDouble(), anyDouble())).thenReturn(0L);
        when(zSetOperations.zCard(any())).thenReturn(2L);
        when(zSetOperations.add(any(), any(), anyDouble())).thenReturn(true);
        when(redisTemplate.expire(any(), anyLong(), any())).thenReturn(true);
        when(logRepository.save(any())).thenReturn(new RateLimitLog());

        RateLimitResponse response = rateLimitService.checkRateLimit(request);

        assertTrue(response.isAllowed());
        assertEquals(2, response.getRemainingRequests());
        assertEquals("Request allowed", response.getMessage());
    }

    @Test
    void checkRateLimit_shouldBlockRequest_whenOverLimit() {
        when(zSetOperations.removeRangeByScore(any(), anyDouble(), anyDouble())).thenReturn(0L);
        when(zSetOperations.zCard(any())).thenReturn(5L);
        when(logRepository.save(any())).thenReturn(new RateLimitLog());

        RateLimitResponse response = rateLimitService.checkRateLimit(request);

        assertFalse(response.isAllowed());
        assertEquals(0, response.getRemainingRequests());
        assertEquals("Rate limit exceeded. Try again later.", response.getMessage());
    }

    @Test
    void checkRateLimit_shouldUseClientConfig_whenExists() {
        ClientConfig config = new ClientConfig();
        config.setClientId("user123");
        config.setMaxRequests(2);
        config.setWindowSeconds(30);

        when(clientConfigRepository.findById("user123")).thenReturn(Optional.of(config));
        when(zSetOperations.removeRangeByScore(any(), anyDouble(), anyDouble())).thenReturn(0L);
        when(zSetOperations.zCard(any())).thenReturn(2L);
        when(logRepository.save(any())).thenReturn(new RateLimitLog());

        RateLimitResponse response = rateLimitService.checkRateLimit(request);

        assertFalse(response.isAllowed());
    }

    @Test
    void getClientLogs_shouldReturnLogs() {
        RateLimitLog log = new RateLimitLog();
        log.setClientId("user123");
        when(logRepository.findByClientId("user123")).thenReturn(List.of(log));

        List<RateLimitLog> logs = rateLimitService.getClientLogs("user123");

        assertEquals(1, logs.size());
        assertEquals("user123", logs.get(0).getClientId());
    }
}