package com.vanshika.api_rate_limiter_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a registered user in the system with an optional tier assignment.
 * 
 * Users are the primary entities for identity-based rate limiting.
 * A user can be assigned a tier (free, gold, premium) which dictates their
 * specific rate limits.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {

    /**
     * Unique identifier for the user (e.g., "alice").
     * Used as a key for lookups and in rate limit keys (user:userId).
     */
    private String userId;

    /**
     * The display name of the user.
     * Purely for identification purposes in the management API.
     */
    // private String name;

    /**
     * The tier assigned to this user (e.g., "free", "gold", "premium").
     * Can be null if the user has no specific tier assigned, in which case
     * they fall back to default limits.
     */
    private String tier;
}
