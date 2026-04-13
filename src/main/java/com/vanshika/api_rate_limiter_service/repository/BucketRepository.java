package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;

/**
 * Interface for token bucket storage.
 *
 * Supports both in-memory (single-instance) and distributed (Redis) backends.
 * The two default methods — consumeToken and getStatus — centralise the
 * token-bucket logic so callers never touch TokenBucket directly.
 *
 * In-memory implementation: default methods delegate to getBucket + TokenBucket.
 * Redis implementation:      overrides both with an atomic Lua script.
 */
public interface BucketRepository {

    /**
     * Retrieves or creates the token bucket for the given key.
     * Used internally by the default consumeToken / getStatus implementations.
     * Redis implementation may return null — prefer consumeToken() for new code.
     */
    TokenBucket getBucket(String key, long capacity, long refillTokens, long windowSeconds);

    /**
     * Clears a bucket entry, resetting the rate limit for that key.
     */
    void removeBucket(String key);

    // ─────────────────────────────────────────────────────────────────────────
    // High-level operations — prefer these over raw getBucket()
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Atomically consumes one token and returns the resulting status snapshot.
     * This is the primary entry point for the interceptor / rate-limit path.
     *
     * Default: delegates to getBucket + tryConsume (in-memory path).
     * Redis override: executes a Lua script for atomicity across JVM instances.
     */
    default RateLimitStatus consumeToken(String key,
                                         long capacity,
                                         long refillTokens,
                                         long windowSeconds) {
        TokenBucket bucket = getBucket(key, capacity, refillTokens, windowSeconds);
        boolean allowed   = bucket.tryConsume();
        long remaining    = bucket.getRemainingTokens();
        long resetSeconds = bucket.getSecondsUntilRefill();
        return new RateLimitStatus(key, remaining, capacity, resetSeconds, allowed);
    }

    /**
     * Returns the current bucket status WITHOUT consuming a token.
     * Used by the /status endpoint (read-only inspection).
     *
     * Default: delegates to getBucket + read-only getters.
     * Redis override: executes a read-only Lua script.
     */
    default RateLimitStatus getStatus(String key,
                                      long capacity,
                                      long refillTokens,
                                      long windowSeconds) {
        TokenBucket bucket = getBucket(key, capacity, refillTokens, windowSeconds);
        long remaining    = bucket.getRemainingTokens();
        long resetSeconds = bucket.getSecondsUntilRefill();
        return new RateLimitStatus(key, remaining, capacity, resetSeconds, true);
    }
}
