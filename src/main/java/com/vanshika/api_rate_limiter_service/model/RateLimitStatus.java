package com.vanshika.api_rate_limiter_service.model;

/**
 * RATE LIMIT STATUS DTO
 * 
 * Why this class exists?
 * 1. Business Logic State: Holds everything the caller needs at once.
 * 2. Decoupling: Controller doesn't need to touch TokenBucket methods directly.
 * 3. Immutable: Values are final after creation for better thread safety.
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
