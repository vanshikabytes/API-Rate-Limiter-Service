package com.vanshika.api_rate_limiter_service.model;

/**
 * Snapshot of a client's rate limit state.
 * 
 * We use this DTO to pass usage data between the service and interceptor layers 
 * without exposing the internal TokenBucket implementation.
 */
public class RateLimitStatus {

    private final String key;
    private final long remainingTokens;
    private final long capacity;
    private final long resetSeconds;
    private final boolean allowed;

    public RateLimitStatus(String key, long remainingTokens, long capacity, long resetSeconds, boolean allowed) {
        this.key = key;
        this.remainingTokens = remainingTokens;
        this.capacity = capacity;
        this.resetSeconds = resetSeconds;
        this.allowed = allowed;
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

    public long getResetSeconds() {
        return resetSeconds;
    }

    public boolean isAllowed() {
        return allowed;
    }
}
