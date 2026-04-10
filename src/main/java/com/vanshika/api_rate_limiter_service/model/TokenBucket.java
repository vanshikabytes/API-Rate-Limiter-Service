package com.vanshika.api_rate_limiter_service.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A true Token Bucket implementation with continuous refill.
 * Tokens are added gradually over time rather than in bursts at window boundaries.
 */
public class TokenBucket {
    private static final Logger logger = LoggerFactory.getLogger(TokenBucket.class);

    private final long capacity;
    private final double refillRatePerSecond;
    private double tokens;
    private long lastRefillTime; // In nanoseconds

    /**
     * Creates a new TokenBucket.
     *
     * @param capacity      Maximum number of tokens (requests) allowed in the bucket.
     * @param refillTokens  Number of tokens added over the duration of a window.
     * @param windowSeconds Duration in seconds for the refill cycle.
     */
    public TokenBucket(long capacity, long refillTokens, long windowSeconds) {
        this.capacity = capacity;
        // refillRatePerSecond = tokens / seconds
        this.refillRatePerSecond = windowSeconds > 0 ? (double) refillTokens / windowSeconds : 0;
        this.tokens = capacity;
        this.lastRefillTime = System.nanoTime();
    }

    public synchronized boolean tryConsume() {
        refill();
        
        logger.debug("Tokens before consume: {}", tokens);
        
        if (tokens >= 1.0) {
            tokens -= 1.0;
            logger.debug("Tokens after consume: {}", tokens);
            return true;
        }
        return false;
    }

    public synchronized long getRemainingTokens() {
        refill();
        return (long) Math.floor(tokens);
    }

    public synchronized long getSecondsUntilRefill() {
        refill();
        if (tokens >= 1.0) {
            return 0;
        }
        // seconds = tokens_needed / refillRate
        return refillRatePerSecond > 0 ? (long) Math.ceil((1.0 - tokens) / refillRatePerSecond) : 0;
    }

    public long getCapacity() {
        return capacity;
    }

    public synchronized boolean isExpired() {
        long elapsedNanos = System.nanoTime() - lastRefillTime;
        return elapsedNanos > 300L * 1_000_000_000L; // 5 minutes in nanoseconds
    }

    /**
     * Continuous refill logic.
     * Tokens added = (timeElapsedSinceLastRefill * refillRatePerSecond)
     */
    private void refill() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastRefillTime) / 1_000_000_000.0;
        
        if (elapsedSeconds > 0) {
            double tokensToAdd = elapsedSeconds * refillRatePerSecond;
            tokens = Math.min(capacity, tokens + tokensToAdd);
            lastRefillTime = now;
            logger.debug("Refilled tokens. Current tokens: {}", tokens);
        }
    }
}