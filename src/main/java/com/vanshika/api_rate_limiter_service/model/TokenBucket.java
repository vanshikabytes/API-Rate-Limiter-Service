package com.vanshika.api_rate_limiter_service.model;

import java.time.Duration;
import java.time.Instant;

public class TokenBucket {

    private final long capacity;
    private final double refillRatePerSecond;
    private final long windowDurationSeconds;

    private double tokens;
    private Instant lastRefillTime;

    public TokenBucket(long capacity, long refillRate, long windowDurationSeconds) {
        this.capacity = capacity;
        this.windowDurationSeconds = windowDurationSeconds;
        this.refillRatePerSecond = (double) refillRate / windowDurationSeconds;
        this.tokens = (double) capacity;
        this.lastRefillTime = Instant.now();
    }

    public synchronized boolean tryConsume() {
        refill();

        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        Instant now = Instant.now();
        long nanosElapsed = Duration.between(lastRefillTime, now).toNanos();

        if (nanosElapsed > 0) {
            double secondsElapsed = nanosElapsed / 1_000_000_000.0;
            double tokensToAdd = secondsElapsed * refillRatePerSecond;

            tokens = Math.min((double) capacity, tokens + tokensToAdd);
            lastRefillTime = now;
        }
    }

    public synchronized long getRemainingTokens() {
        refill();
        return (long) Math.floor(tokens);
    }

    public long getCapacity() {
        return capacity;
    }

    public long getWindowDurationSeconds() {
        return windowDurationSeconds;
    }

    public synchronized long getResetTimeSeconds() {
        refill();
        double missingTokens = (double) capacity - tokens;
        if (missingTokens <= 0) {
            return Instant.now().getEpochSecond();
        }
        long secondsToFull = (long) Math.ceil(missingTokens / refillRatePerSecond);
        return Instant.now().getEpochSecond() + secondsToFull;
    }

    public synchronized long getRetryAfterSeconds() {
        refill();
        if (tokens >= 1.0) {
            return 0;
        }
        double tokensNeeded = 1.0 - tokens;
        return (long) Math.ceil(tokensNeeded / refillRatePerSecond);
    }

    public synchronized boolean isExpired(long ttlSeconds) {
        Instant now = Instant.now();
        long secondsElapsed = now.getEpochSecond() - lastRefillTime.getEpochSecond();
        return secondsElapsed > ttlSeconds;
    }
}