package com.vanshika.api_rate_limiter_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class RateLimiterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testDefaultWindowHeader() throws Exception {
        mockMvc.perform(get("/api/rate-limit/user:test"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Capacity"))
                .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    void testSecondWindowExceeded() throws Exception {
        // user:second has limit 10
        String key = "user:second_test";
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/rate-limit/second/" + key))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/rate-limit/second/" + key))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void testSeparateWindows() throws Exception {
        String key = "user:separate_test";

        // Use 10 tokens in 'second' window
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/rate-limit/second/" + key))
                    .andExpect(status().isOk());
        }

        // 'second' window should be blocked
        mockMvc.perform(get("/api/rate-limit/second/" + key))
                .andExpect(status().is(429));

        // 'minute' window should still be open (limit 100)
        mockMvc.perform(get("/api/rate-limit/minute/" + key))
                .andExpect(status().isOk());
    }
}
