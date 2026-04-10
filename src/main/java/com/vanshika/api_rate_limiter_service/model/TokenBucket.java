package com.vanshika.api_rate_limiter_service.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class TokenBucket {
    private final long capacity;
    private final long windowSeconds;
    private final AtomicLong tokens;
    private volatile Instant lastRefillTime;

    /**
     * Creates a new TokenBucket.
     *
     * @param capacity      Maximum number of tokens (requests) allowed per window.
     * @param windowSeconds Duration in seconds before the bucket fully refills.
     */
    public TokenBucket(long capacity, long windowSeconds) {
        this.capacity = capacity;
        this.windowSeconds = windowSeconds;
        this.tokens = new AtomicLong(capacity);
        this.lastRefillTime = Instant.now();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens.get() > 0) {
            tokens.decrementAndGet();
            return true;
        }
        return false;
    }

    public synchronized long getRemainingTokens() {
        refill();
        return tokens.get();
    }

    public synchronized long getSecondsUntilRefill() {
        Instant now = Instant.now();
        long elapsed = now.getEpochSecond() - lastRefillTime.getEpochSecond();
        return Math.max(0, windowSeconds - elapsed);
    }

    public long getCapacity() {
        return capacity;
    }

    public synchronized boolean isExpired() {
        long elapsed = Instant.now().getEpochSecond() - lastRefillTime.getEpochSecond();
        return elapsed > 300;

    }

    // REFILL LOGIC
    private void refill() {
        // windowSeconds == 0 means "never auto-refill" (used in tests and no-refill configs)
        if (windowSeconds == 0) return;

        Instant now = Instant.now();
        long elapsed = now.getEpochSecond() - lastRefillTime.getEpochSecond();
        if (elapsed >= windowSeconds) {
            tokens.set(capacity); // full refill after window
            lastRefillTime = now;
        }
    }
}