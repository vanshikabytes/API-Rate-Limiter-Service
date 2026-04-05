package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;

/**
 * CONTROLLER TEST — Task 2: RateLimiterController
 *
 * Uses MockMvc to test REST endpoints without starting a full server.
 * This is fast and verifies HTTP mapping, JSON serialization, and path variables.
 */
@WebMvcTest(RateLimiterController.class)
class RateLimiterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @Test
    void shouldGetStatus() throws Exception {
        RateLimitStatus mockStatus = new RateLimitStatus("user:1", 5, 5, 60, true);
        when(rateLimiterService.getCurrentStatus(anyString())).thenReturn(mockStatus);

        mockMvc.perform(get("/api/rate-limit/status/user:1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Current rate limit status retrieved successfully"));
    }

    @Test
    void shouldResetBucket() throws Exception {
        RateLimitStatus mockStatus = new RateLimitStatus("user:1", 5, 5, 0, true);
        when(rateLimiterService.getCurrentStatus(anyString())).thenReturn(mockStatus);

        mockMvc.perform(post("/api/rate-limit/reset/user:1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Rate limit reset successfully for key: user:1"));
    }

    @Test
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/rate-limit/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("OK"));
    }
}
