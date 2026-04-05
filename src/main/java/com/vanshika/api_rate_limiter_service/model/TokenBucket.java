package com.vanshika.api_rate_limiter_service.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TOKEN BUCKET — Core rate limiting model (Phase 1, in-memory)
 *
 * Algorithm:
 *  - Each client gets a "bucket" with a fixed capacity of tokens.
 *  - Every request consumes 1 token.
 *  - Tokens refill at a fixed rate per second (up to capacity).
 *  - If no tokens remain, the request is rejected (HTTP 429).
 *
 * Thread Safety Design:
 *  - `tokens` uses AtomicLong for lock-free, atomic reads/writes.
 *  - `lastRefillTime` is marked `volatile` so that all threads always
 *    see the most recently written value without stale cache copies.
 *  - `tryConsume()` and `getRemainingTokens()` are synchronized so that
 *    the refill + consume / refill + read sequence stays atomic.
 *    Without synchronization, two threads could both read a stale token
 *    count, both decide "allow", and together consume more tokens than allowed.
 *  - `isExpired()` is synchronized to prevent reading a partially-updated
 *    `lastRefillTime` while another thread is inside refill().
 */
public class TokenBucket {

    private final long capacity;           // max tokens this bucket can hold
    private final long refillRatePerSecond; // how many tokens are added per second

    // AtomicLong gives us a thread-safe counter without a full synchronized block
    private final AtomicLong tokens;

    // volatile: guarantees visibility across threads.
    // Without volatile, one thread's write to lastRefillTime might not be visible
    // to another thread, causing the same time window to be refilled twice.
    private volatile Instant lastRefillTime;

    public TokenBucket(long capacity, long refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.tokens = new AtomicLong(capacity); // start full
        this.lastRefillTime = Instant.now();
    }

    /**
     * Attempts to consume one token.
     *
     * synchronized: ensures refill() and the consume decision execute
     * as one atomic unit. Without this, two threads could both pass the
     * tokens.get() > 0 check simultaneously and both consume, allowing
     * more requests than permitted.
     *
     * @return true if a token was consumed (request allowed), false if bucket is empty (429)
     */
    public synchronized boolean tryConsume() {
        refill(); // top up tokens based on elapsed time before deciding

        if (tokens.get() > 0) {
            tokens.decrementAndGet();
            return true; // request allowed
        }
        return false; // request rejected — bucket empty
    }

    /**
     * Returns the current token count after topping up.
     * Used by the controller to populate X-RateLimit-Remaining header.
     *
     * synchronized: must hold the same lock as tryConsume() to ensure
     * the returned value reflects the same post-refill state.
     *
     * @return number of tokens remaining
     */
    public synchronized long getRemainingTokens() {
        refill();
        return tokens.get();
    }

    /**
     * Returns how many seconds until the next refill cycle.
     * Used by the controller to populate X-RateLimit-Reset and Retry-After headers.
     *
     * A "refill cycle" happens every second (since we refill per second).
     * Seconds until reset = 1 - (fractional seconds elapsed since last refill).
     *
     * @return seconds until next token is added (minimum 1 second)
     */
    public synchronized long getSecondsUntilRefill() {
        Instant now = Instant.now();
        long millisSinceLastRefill = now.toEpochMilli() - lastRefillTime.toEpochMilli();
        long millisUntilNextRefill = 1000 - (millisSinceLastRefill % 1000);
        // Convert to whole seconds, minimum 1 so we never return 0 to clients
        return Math.max(1, (millisUntilNextRefill + 999) / 1000);
    }

    /**
     * Returns the bucket's maximum capacity.
     * Used by the controller to populate X-RateLimit-Capacity header.
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Returns true if this bucket has been inactive for more than 5 minutes.
     * Used by the scheduled cleanup task in InMemoryBucketRepository.
     *
     * synchronized: reads lastRefillTime which could be written by refill()
     * in another thread. Synchronizing prevents a data race on the Instant read.
     *
     * @return true if the bucket should be evicted from memory
     */
    public synchronized boolean isExpired() {
        Instant now = Instant.now();
        long secondsElapsed = now.getEpochSecond() - lastRefillTime.getEpochSecond();
        return secondsElapsed > 300; // 5 minutes of inactivity → evict
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Refills tokens proportional to the time elapsed since the last refill.
     *
     * Called from within synchronized methods, so no additional locking is needed.
     * Must NOT be called from un-synchronized context.
     */
    private void refill() {
        Instant now = Instant.now();
        long secondsElapsed = now.getEpochSecond() - lastRefillTime.getEpochSecond();

        if (secondsElapsed > 0) {
            long tokensToAdd = secondsElapsed * refillRatePerSecond;

            // Cap at capacity — we don't store more than what the bucket can hold
            long newTokenCount = Math.min(capacity, tokens.get() + tokensToAdd);

            tokens.set(newTokenCount);
            lastRefillTime = now; // mark when we last topped up
        }
    }
}