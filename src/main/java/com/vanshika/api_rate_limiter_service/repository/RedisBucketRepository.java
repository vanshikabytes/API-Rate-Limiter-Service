package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DISTRIBUTED rate limit repository backed by Redis.
 *
 * Active on all profiles EXCEPT "local" (@Primary wins when Redis is available).
 *
 * ─── Why this is correct ────────────────────────────────────────────────────
 * InMemoryBucketRepository stores token state inside the JVM heap.
 * When you run 3 instances of this service, each has its own counter —
 * a user can make 3× the configured limit before hitting a 429. That is
 * wrong for a rate limiter that's supposed to be fair.
 *
 * This implementation stores the token count and last-refill timestamp in
 * Redis using a two-key schema:
 *
 *   rate_limit:{key}:tokens    → current token count (float stored as string)
 *   rate_limit:{key}:timestamp → last refill time in epoch-milliseconds
 *
 * All reads, refills, and decrements happen inside a single Lua script,
 * which Redis executes atomically. No two JVMs can simultaneously read and
 * both decrement the same key. Race condition eliminated.
 *
 * Keys expire after `windowSeconds` of inactivity (EXPIRE command in script),
 * so idle users do not accumulate memory in Redis.
 * ────────────────────────────────────────────────────────────────────────────
 */
@Repository
@Primary                          // wins over InMemoryBucketRepository when both are on classpath
@Profile("!local")                // disabled when running with --spring.profiles.active=local
public class RedisBucketRepository implements BucketRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisBucketRepository.class);

    // Redis key templates — human-readable, easy to inspect with redis-cli
    private static final String TOKEN_KEY_TEMPLATE = "rate_limit:%s:tokens";
    private static final String TS_KEY_TEMPLATE    = "rate_limit:%s:timestamp";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> consumeScript;
    private final DefaultRedisScript<List> statusScript;

    public RedisBucketRepository(
            StringRedisTemplate redisTemplate,
            @Qualifier("consumeTokenScript") DefaultRedisScript<List> consumeScript,
            @Qualifier("statusScript")       DefaultRedisScript<List> statusScript) {
        this.redisTemplate  = redisTemplate;
        this.consumeScript  = consumeScript;
        this.statusScript   = statusScript;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BucketRepository contract — high-level methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Atomically consumes one token via Lua script.
     *
     * The Lua script in RedisConfig.consumeTokenScript():
     *   1. Reads current tokens + last-refill timestamp
     *   2. Calculates elapsed time → tokens earned
     *   3. Caps token count at capacity
     *   4. If tokens >= 1  → decrements, returns allowed=1
     *      Else              → returns allowed=0 (429 path)
     *   5. Persists updated tokens + timestamp with TTL
     *
     * @return RateLimitStatus — same contract as the in-memory path
     */
    @Override
    public RateLimitStatus consumeToken(String key,
                                        long capacity,
                                        long refillTokens,
                                        long windowSeconds) {
        // refillRate = tokens added per second over the window
        double refillRatePerSecond = windowSeconds > 0
                ? (double) refillTokens / windowSeconds : 0.0;

        String tokensKey = TOKEN_KEY_TEMPLATE.formatted(key);
        String tsKey     = TS_KEY_TEMPLATE.formatted(key);
        long   nowMs     = System.currentTimeMillis();

        List<?> result = executeWithFallback(() ->
                redisTemplate.execute(
                        consumeScript,
                        List.of(tokensKey, tsKey),
                        String.valueOf(capacity),
                        String.valueOf(refillRatePerSecond),
                        String.valueOf(nowMs),
                        String.valueOf(windowSeconds)
                )
        );

        boolean allowed   = toLong(result, 0) == 1L;
        long    remaining = toLong(result, 1);

        long resetSeconds = 0;
        if (!allowed && refillRatePerSecond > 0) {
            resetSeconds = (long) Math.ceil(1.0 / refillRatePerSecond);
        }

        log.debug("[Redis] consumeToken key={} allowed={} remaining={}", key, allowed, remaining);

        return new RateLimitStatus(key, remaining, capacity, resetSeconds, allowed);
    }

    /**
     * Reads current token state WITHOUT decrementing.
     * Used by the /status endpoint — safe to call as many times as needed.
     */
    @Override
    public RateLimitStatus getStatus(String key,
                                     long capacity,
                                     long refillTokens,
                                     long windowSeconds) {
        double refillRatePerSecond = windowSeconds > 0
                ? (double) refillTokens / windowSeconds : 0.0;

        String tokensKey = TOKEN_KEY_TEMPLATE.formatted(key);
        String tsKey     = TS_KEY_TEMPLATE.formatted(key);
        long   nowMs     = System.currentTimeMillis();

        List<?> result = executeWithFallback(() ->
                redisTemplate.execute(
                        statusScript,
                        List.of(tokensKey, tsKey),
                        String.valueOf(capacity),
                        String.valueOf(refillRatePerSecond),
                        String.valueOf(nowMs),
                        String.valueOf(windowSeconds)
                )
        );

        long remaining    = toLong(result, 0);
        long resetSeconds = toLong(result, 1);

        log.debug("[Redis] getStatus key={} remaining={} resetSeconds={}", key, remaining, resetSeconds);

        return new RateLimitStatus(key, remaining, capacity, resetSeconds, true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BucketRepository contract — lower-level methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Not used in the Redis path — state lives in Redis, not in a Java object.
     * Returns null intentionally; callers should always prefer consumeToken().
     *
     * Kept to satisfy the BucketRepository interface so existing code that
     * calls getBucket() directly still compiles (e.g., legacy service code).
     */
    @Override
    public TokenBucket getBucket(String key, long capacity, long refillTokens, long windowSeconds) {
        // Redis doesn't vend a TokenBucket object — the state is remote.
        // Returning null is intentional: callers must switch to consumeToken().
        log.warn("[Redis] getBucket() called directly for key={}. Prefer consumeToken().", key);
        return null;
    }

    /**
     * Deletes both Redis keys for this identifier, effectively resetting the
     * rate limit to a fresh full bucket on the next request.
     */
    @Override
    public void removeBucket(String key) {
        String tokensKey = TOKEN_KEY_TEMPLATE.formatted(key);
        String tsKey     = TS_KEY_TEMPLATE.formatted(key);
        redisTemplate.delete(List.of(tokensKey, tsKey));
        log.info("[Redis] Bucket reset for key={}", key);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Executes a Redis operation with a graceful fallback.
     *
     * BONUS — Fallback strategy:
     * If Redis is temporarily unreachable (connection refused, timeout), we log
     * a warning and ALLOW the request rather than blocking all traffic because
     * of an infrastructure hiccup. This is the "fail-open" policy, which is the
     * correct default for a rate limiter backing a public API.
     *
     * To switch to "fail-closed" (reject on Redis failure), change the catch
     * block to throw a RateLimitExceededException or return a denied result.
     */
    private List<?> executeWithFallback(java.util.function.Supplier<List<?>> redisCall) {
        try {
            List<?> result = redisCall.get();
            if (result == null || result.isEmpty()) {
                log.warn("[Redis] Lua script returned null/empty — fail-open.");
                return List.of(1L, Long.MAX_VALUE); // allow
            }
            return result;
        } catch (Exception ex) {
            log.error("[Redis] Redis unavailable: {} — failing OPEN (request allowed).", ex.getMessage());
            // Fail-open: return allowed=1, remaining=MAX so the request proceeds.
            // Change to List.of(0L, 0L) for fail-closed behaviour.
            return List.of(1L, Long.MAX_VALUE);
        }
    }

    /**
     * Safely extracts a long from the Lua script result list.
     * The Redis client returns Long values for Lua integer returns.
     */
    private long toLong(List<?> result, int index) {
        if (result == null || result.size() <= index || result.get(index) == null) {
            return 0L;
        }
        Object val = result.get(index);
        if (val instanceof Long l) return l;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (NumberFormatException e) { return 0L; }
    }
}
