package com.vanshika.api_rate_limiter_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating a new user.
 * 
 * Includes validation to ensure required identity information is provided
 * at the time of registration.
 */
@Getter
@Setter
public class CreateUserRequest {

    /**
     * Unique identifier for the user.
     * Must not be blank as it's used for rate limit keys.
     */
    @NotBlank(message = "userId must not be blank")
    private String userId;

    /**
     * Display name for the user.
     * Required for administrative tracking.
     */
    // @NotBlank(message = "name must not be blank")
    // private String name;

    /**
     * Initial tier for the user (optional).
     * Character-based string (e.g., "free") used to map to TierConfig.
     */
    private String tier;
}
