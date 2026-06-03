package com.vanshika.api_rate_limiter_service.model;

/**
 * Data Transfer Object for rate limit metadata.
 * 
 * Provides clients with machine-readable information about their current 
 * usage and remaining quota.
 */
public class RateLimitResponse {

    private final String key;
    private final long remainingTokens;
    private final long capacity;

    public RateLimitResponse(String key, long remainingTokens, long capacity) {
        this.key = key;
        this.remainingTokens = remainingTokens;
        this.capacity = capacity;
    }

    public String getKey() {
        return key;
    }

    public long getRemainingTokens() {
        return remainingTokens;
    }

    public long getCapacity() {
        return capacity;
    }
}
