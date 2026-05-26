package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.model.User;
import com.vanshika.api_rate_limiter_service.repository.UserRepository;
import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for UserController.
 * Verifies that updating a user tier automatically invalidates their rate limiting bucket.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @Test
    void shouldUpdateTierAndAutoInvalidateBucket() throws Exception {
        // Given
        User mockUser = new User("alice", "gold");
        when(userRepository.updateTier(eq("alice"), eq("gold"))).thenReturn(mockUser);

        // When/Then
        mockMvc.perform(patch("/api/users/alice/tier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tier\": \"gold\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User tier updated to gold. Rate limit bucket refreshed automatically."))
                .andExpect(jsonPath("$.data.userId").value("alice"))
                .andExpect(jsonPath("$.data.tier").value("gold"));

        // Verify that rateLimiterService.reset("user:alice") was called automatically (Task 1)
        verify(rateLimiterService, times(1)).reset("user:alice");
    }
}
