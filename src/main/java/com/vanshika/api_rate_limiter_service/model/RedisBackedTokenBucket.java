package com.vanshika.api_rate_limiter_service.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

/**
 * A Redis-backed TokenBucket that stores token state in Redis for distributed consistency.
 *
 * Extends TokenBucket to satisfy the existing BucketRepository contract without
 * changing any interface or the parent algorithm class.
 *
 * All public operations (tryConsume, getRemainingTokens, getSecondsUntilRefill)
 * are overridden and delegated to Redis via an atomic Lua script. The parent
 * class fields are initialised by the super() call but are never read — only
 * the Redis-side state matters.
 *
 * Lua script semantics (executed atomically inside Redis):
 *   1. Read current tokens + last_refill_time_ms from HASH
 *   2. Calculate elapsed time, add proportional tokens (capped at capacity)
 *   3. Optionally consume one token (controlled by ARGV[5])
 *   4. Persist updated HASH and refresh TTL
 *   5. Return [allowed (0|1), floor(remaining_tokens)]
 */
public class RedisBackedTokenBucket extends TokenBucket {

    private static final Logger logger = LoggerFactory.getLogger(RedisBackedTokenBucket.class);

    // ── Lua script ────────────────────────────────────────────────────────────
    // Stored on the class so it is compiled once; shared across all instances
    // via the repository that injects it.
    public static final String LUA_SCRIPT = """
            local key             = KEYS[1]
            local capacity        = tonumber(ARGV[1])
            local refill_rate     = tonumber(ARGV[2])
            local now_ms          = tonumber(ARGV[3])
            local window_seconds  = tonumber(ARGV[4])
            local consume         = tonumber(ARGV[5])

            local data         = redis.call('HMGET', key, 'tokens', 'last_refill_time_ms')
            local tokens       = tonumber(data[1])
            local last_refill  = tonumber(data[2])

            if tokens == nil then
                tokens      = capacity
                last_refill = now_ms
            end

            -- Continuous refill: add tokens proportional to elapsed time
            local elapsed_sec   = (now_ms - last_refill) / 1000.0
            local tokens_to_add = elapsed_sec * refill_rate
            tokens = math.min(capacity, tokens + tokens_to_add)

            -- Optional consumption
            local allowed = 0
            if consume == 1 then
                if tokens >= 1.0 then
                    tokens  = tokens - 1.0
                    allowed = 1
                end
            end

            -- Persist state; set TTL to 2× window for automatic cleanup
            redis.call('HMSET', key, 'tokens', tokens, 'last_refill_time_ms', now_ms)
            redis.call('EXPIRE', key, window_seconds * 2)

            return {allowed, math.floor(tokens)}
            """;

    // ── Instance state ────────────────────────────────────────────────────────

    private final String redisKey;
    private final long   capacity;
    private final double refillRatePerSecond;
    private final long   windowSeconds;

    private final StringRedisTemplate          redisTemplate;
    private final DefaultRedisScript<List>     luaScript;

    /** Cached after the first Lua call so we never call Redis twice per request. */
    private volatile long    cachedRemaining = -1;
    private volatile boolean initialized     = false;
    private volatile boolean fallback        = false;  // true when Redis was unreachable

    public RedisBackedTokenBucket(
            String redisKey,
            long   capacity,
            long   refillTokens,
            long   windowSeconds,
            double refillRatePerSecond,
            StringRedisTemplate      redisTemplate,
            DefaultRedisScript<List> luaScript) {

        // super() is required to satisfy the constructor; parent state is ignored.
        super(capacity, refillTokens, windowSeconds);

        this.redisKey            = redisKey;
        this.capacity            = capacity;
        this.windowSeconds       = windowSeconds;
        this.refillRatePerSecond = refillRatePerSecond;
        this.redisTemplate       = redisTemplate;
        this.luaScript           = luaScript;
    }

    // ── Overridden public methods ─────────────────────────────────────────────

    /**
     * Atomically refills and consumes one token via the Redis Lua script.
     * Caches the post-consumption remaining count for subsequent calls.
     *
     * On Redis failure the method fails-open (returns true) and sets the
     * X-RateLimit-Fallback header via the caller.
     */
    @Override
    public synchronized boolean tryConsume() {
        try {
            List<Long> result = executeLua(true);
            if (result != null && result.size() >= 2) {
                cachedRemaining = result.get(1);
                initialized     = true;
                return result.get(0) == 1L;
            }
        } catch (Exception e) {
            logger.error("[Redis] tryConsume failed for key '{}': {}", redisKey, e.getMessage());
        }
        // fail-open: let the request through
        cachedRemaining = capacity;
        initialized     = true;
        fallback        = true;
        return true;
    }

    /**
     * Returns remaining tokens.
     * Uses the cached value if tryConsume() was already called; otherwise
     * executes a read-only Lua call (consume=0).
     */
    @Override
    public synchronized long getRemainingTokens() {
        if (initialized) return cachedRemaining;
        try {
            List<Long> result = executeLua(false);
            if (result != null && result.size() >= 2) {
                cachedRemaining = result.get(1);
                initialized     = true;
                return cachedRemaining;
            }
        } catch (Exception e) {
            logger.error("[Redis] getRemainingTokens failed for key '{}': {}", redisKey, e.getMessage());
        }
        fallback = true;
        return capacity; // fail-open: report full
    }

    /**
     * Returns true when this bucket operated in fail-open mode because Redis
     * was unreachable. The interceptor uses this to add X-RateLimit-Fallback: true.
     */
    public boolean isFallback() {
        return fallback;
    }

    /**
     * Estimates seconds until the next token is available.
     * Returns 0 if at least one token remains; otherwise 1/refillRate seconds.
     */
    @Override
    public synchronized long getSecondsUntilRefill() {
        long remaining = getRemainingTokens();
        if (remaining >= 1) return 0;
        // time to earn one token = 1 / refillRate
        return refillRatePerSecond > 0
                ? (long) Math.ceil(1.0 / refillRatePerSecond)
                : 0;
    }

    /**
     * Redis handles TTL-based expiry; this bucket object is always "fresh".
     */
    @Override
    public synchronized boolean isExpired() {
        return false;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Long> executeLua(boolean consume) {
        return (List<Long>) redisTemplate.execute(
                luaScript,
                List.of(redisKey),
                String.valueOf(capacity),
                String.valueOf(refillRatePerSecond),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(windowSeconds),
                consume ? "1" : "0");
    }
}
