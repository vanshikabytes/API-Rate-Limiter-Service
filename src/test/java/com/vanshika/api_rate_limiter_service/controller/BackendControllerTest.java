package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.repository.BucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * INTEGRATION TEST — Task 2: BackendController + Interceptor
 *
 * Uses @SpringBootTest to load the full application context.
 * This ensures the HandlerInterceptor is active, allowing us to test
 * both the happy-path and the 429 Rate Limit Exceeded path.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BackendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private RateLimiterProperties properties;

    @BeforeEach
    void setup() {
        // Clear all buckets before each test for predictability
        bucketRepository.removeBucket("user:test-1");
    }

    @Test
    void shouldAllowBackendAccessAndIncludeHeaders() throws Exception {
        mockMvc.perform(get("/api/backend/data")
                .header("X-User-Id", "test-1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Capacity"))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldBlockWhenLimitExceeded() throws Exception {
        String userId = "test-1";
        long capacity = properties.getUser().getCapacity();

        // 1. Exhaust the bucket
        for (int i = 0; i < capacity; i++) {
            mockMvc.perform(get("/api/backend/data")
                    .header("X-User-Id", userId))
                    .andExpect(status().isOk());
        }

        // 2. The next request should fail with 429
        mockMvc.perform(get("/api/backend/data")
                .header("X-User-Id", userId))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Rate limit exceeded")));
    }

    @Test
    void shouldReturn404ForNonExistentUser() throws Exception {
        // Our BackendService simulation throws 404 if ID contains '0'
        mockMvc.perform(get("/api/backend/users/10")
                .header("X-User-Id", "test-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not found")));
    }
}
