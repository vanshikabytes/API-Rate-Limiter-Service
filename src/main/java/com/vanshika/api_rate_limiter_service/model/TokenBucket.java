package com.vanshika.api_rate_limiter_service.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class TokenBucket {

    private final long capacity;
    private final long refillRatePerSecond;

    private final AtomicLong tokens;
    private Instant lastRefillTime;

    public TokenBucket(long capacity, long refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
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

    private void refill() {
        Instant now = Instant.now();

        long secondsElapsed = now.getEpochSecond() - lastRefillTime.getEpochSecond();

        if (secondsElapsed > 0) {
            long tokensToAdd = secondsElapsed * refillRatePerSecond;

            long newTokenCount = Math.min(capacity, tokens.get() + tokensToAdd);

            tokens.set(newTokenCount);
            lastRefillTime = now;
        }
    }

    public synchronized long getRemainingTokens() {
        refill();
        return tokens.get();
    }

    public long getCapacity() {
        return capacity;
    }

    public boolean isExpired() {
        Instant now = Instant.now();
        long secondsElapsed = now.getEpochSecond() - lastRefillTime.getEpochSecond();
        return secondsElapsed > 300; // 5 minutes = 300 seconds
    }
}