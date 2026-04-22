package com.ratesentry.ratesentry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratesentry.ratesentry.model.RateLimitRequest;
import com.ratesentry.ratesentry.model.RateLimitResponse;
import com.ratesentry.ratesentry.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = com.ratesentry.ratesentry.controller.RateLimitController.class)
class RateLimitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RateLimitService rateLimitService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void health_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/ratelimit/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("RateSentry is running"));
    }

    @Test
    void checkRateLimit_shouldReturn200_whenAllowed() throws Exception {
        RateLimitRequest request = new RateLimitRequest();
        request.setClientId("user123");
        request.setEndpoint("/api/products");
        request.setMaxRequests(5);
        request.setWindowSeconds(60);

        RateLimitResponse response = new RateLimitResponse();
        response.setAllowed(true);
        response.setRemainingRequests(4);
        response.setMessage("Request allowed");

        when(rateLimitService.checkRateLimit(any())).thenReturn(response);

        mockMvc.perform(post("/api/ratelimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.remainingRequests").value(4));
    }

    @Test
    void checkRateLimit_shouldReturn429_whenBlocked() throws Exception {
        RateLimitRequest request = new RateLimitRequest();
        request.setClientId("user123");
        request.setEndpoint("/api/products");
        request.setMaxRequests(5);
        request.setWindowSeconds(60);

        RateLimitResponse response = new RateLimitResponse();
        response.setAllowed(false);
        response.setRemainingRequests(0);
        response.setMessage("Rate limit exceeded");

        when(rateLimitService.checkRateLimit(any())).thenReturn(response);

        mockMvc.perform(post("/api/ratelimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    void checkRateLimit_shouldReturn400_whenInvalidRequest() throws Exception {
        RateLimitRequest request = new RateLimitRequest();
        request.setClientId("");
        request.setEndpoint("");
        request.setMaxRequests(0);
        request.setWindowSeconds(0);

        mockMvc.perform(post("/api/ratelimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}