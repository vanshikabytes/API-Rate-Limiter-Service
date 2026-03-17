package com.vanshika.api_rate_limiter_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.boot.test.mock.mockito.MockBean;
import com.vanshika.api_rate_limiter_service.repository.RedisBucketRepository;
import com.vanshika.api_rate_limiter_service.service.RuleEngineService;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;

@SpringBootTest
@AutoConfigureMockMvc
public class RateLimiterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisBucketRepository redisBucketRepository;

    @MockBean
    private RuleEngineService ruleEngine;

    @BeforeEach
    void setup() {
        // Default: always allow
        when(redisBucketRepository.tryConsume(anyString(), anyLong(), anyLong(), anyLong(), anyInt()))
                .thenReturn(List.of(1L, 100L));
    }

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
        
        // Mock: 10 allowed, then blocked
        var responses = new java.util.ArrayList<List<Long>>();
        for (int i = 0; i < 10; i++) responses.add(List.of(1L, (long)(9-i)));
        
        when(redisBucketRepository.tryConsume(anyString(), anyLong(), anyLong(), anyLong(), eq(1)))
                .thenReturn(responses.get(0), responses.subList(1, 10).toArray(new List[0]))
                .thenReturn(List.of(0L, 0L));

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

        // Mock: 10 allowed for 'second', then blocked
        var responses = new java.util.ArrayList<List<Long>>();
        for (int i = 0; i < 10; i++) responses.add(List.of(1L, (long)(9-i)));
        
        // Set up different behavior for different windows/keys if possible, 
        // but since we call different endpoints, we can just use the sequence.
        when(redisBucketRepository.tryConsume(contains("second"), anyLong(), anyLong(), anyLong(), eq(1)))
                .thenReturn(responses.get(0), responses.subList(1, 10).toArray(new List[0]))
                .thenReturn(List.of(0L, 0L));
                
        when(redisBucketRepository.tryConsume(contains("minute"), anyLong(), anyLong(), anyLong(), eq(1)))
                .thenReturn(List.of(1L, 99L));

        // Use 10 tokens in 'second' window
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/rate-limit/second/" + key))
                    .andExpect(status().isOk());
        }

        // 'second' window should be blocked
        mockMvc.perform(get("/api/rate-limit/second/" + key))
                .andExpect(status().is(429));

        // 'minute' window should still be open
        mockMvc.perform(get("/api/rate-limit/minute/" + key))
                .andExpect(status().isOk());
    }
}
