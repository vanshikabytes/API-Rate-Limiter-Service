package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.RedisBackedTokenBucket;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Redis-backed implementation of BucketRepository.
 *
 * Active only when rate-limiter.storage=redis in application.yaml.
 * All rate-limit state lives in Redis, so every application instance
 * (on any port / server) shares the same counters — this is what makes
 * horizontal scaling work correctly.
 *
 * Key design decisions:
 *  - Lua script ensures refill + consume is atomic (no race conditions).
 *  - Redis HASH per client key stores: tokens, last_refill_time_ms.
 *  - TTL is set to 2× windowSeconds for automatic cleanup (no @Scheduled needed).
 *  - On Redis failure, the bucket fails-open (request is allowed) and logs an error.
 *    The interceptor will also add X-RateLimit-Fallback: true to the response.
 *
 * Key pattern: rate_limit:{clientKey}
 *   Example:   rate_limit:user:alice
 */
@Repository
@ConditionalOnProperty(name = "rate-limiter.storage", havingValue = "redis")
public class RedisBucketRepository implements BucketRepository {

    private static final Logger logger = LoggerFactory.getLogger(RedisBucketRepository.class);
    private static final String KEY_PREFIX = "rate_limit:";

    private final StringRedisTemplate      redisTemplate;
    private final DefaultRedisScript<List> rateLimitScript;

    public RedisBucketRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        // Compile the Lua script once at startup — Spring caches the SHA1
        // and uses EVALSHA on subsequent calls (faster than EVAL every time).
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(RedisBackedTokenBucket.LUA_SCRIPT);
        script.setResultType(List.class);
        this.rateLimitScript = script;

        logger.info("[Redis] RedisBucketRepository initialised. Key prefix='{}'", KEY_PREFIX);
    }

    /**
     * Returns a RedisBackedTokenBucket for the given key.
     *
     * The bucket object is a lightweight handle; actual Redis I/O happens
     * inside tryConsume() / getRemainingTokens() on the returned bucket.
     *
     * @param key           Client identifier key (e.g., "user:alice")
     * @param capacity      Max tokens
     * @param refillTokens  Tokens added per windowSeconds
     * @param windowSeconds Refill window duration in seconds
     */
    @Override
    public TokenBucket getBucket(String key, long capacity, long refillTokens, long windowSeconds) {
        String redisKey = KEY_PREFIX + key;
        double refillRatePerSecond = windowSeconds > 0
                ? (double) refillTokens / windowSeconds
                : 0.0;

        return new RedisBackedTokenBucket(
                redisKey,
                capacity,
                refillTokens,
                windowSeconds,
                refillRatePerSecond,
                redisTemplate,
                rateLimitScript);
    }

    /**
     * Deletes the Redis key, effectively resetting the rate limit for that client.
     * On Redis failure the error is logged but not re-thrown (admin reset is not critical).
     */
    @Override
    public void removeBucket(String key) {
        try {
            Boolean deleted = redisTemplate.delete(KEY_PREFIX + key);
            logger.debug("[Redis] removeBucket key='{}' deleted={}", KEY_PREFIX + key, deleted);
        } catch (Exception e) {
            logger.error("[Redis] removeBucket failed for key '{}': {}", key, e.getMessage());
        }
    }
}
