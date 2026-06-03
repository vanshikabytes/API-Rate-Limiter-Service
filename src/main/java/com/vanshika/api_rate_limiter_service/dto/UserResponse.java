package com.vanshika.api_rate_limiter_service.dto;

import java.time.Instant;

/**
 * Typed response for user-related data.
 * 
 * Using a dedicated DTO instead of a generic Map ensures compile-time type safety 
 * and predictable JSON structure for API consumers.
 */
public class UserResponse {

    private final String id;
    private final String name;
    private final String email;
    private final Instant createdAt;

    public UserResponse(String id, String name, String email, Instant createdAt) {
        this.id        = id;
        this.name      = name;
        this.email     = email;
        this.createdAt = createdAt;
    }

    // Getters for Jackson serialization.
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
