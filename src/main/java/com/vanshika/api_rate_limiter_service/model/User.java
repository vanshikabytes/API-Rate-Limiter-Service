package com.vanshika.api_rate_limiter_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a registered user in the system with an optional tier assignment.
 * 
 * Users are the primary entities for identity-based rate limiting.
 * A user can be assigned a tier (FREE, PRO, ENTERPRISE, UNLIMITED) which dictates their
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
     * The tier assigned to this user (e.g., "FREE", "PRO", "ENTERPRISE").
     * Can be null if the user has no specific tier assigned, in which case
     * they fall back to default limits.
     */
    private String tier;

    /**
     * Optional expiration timestamp for the current tier.
     * If set and in the past, the system automatically treats this user as FREE tier.
     * Null means the tier does not expire.
     */
    private LocalDateTime tierExpiresAt;
}
