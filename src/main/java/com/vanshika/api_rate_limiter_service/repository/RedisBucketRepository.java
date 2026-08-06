package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.RedisBackedTokenBucket;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "rate-limiter.storage", havingValue = "redis")
public class RedisBucketRepository implements BucketRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisBucketRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public TokenBucket getBucket(String key, long capacity, long refillTokens, long windowSeconds) {
        // Return a dummy TokenBucket to satisfy the interface, 
        // but override methods to route to RedisBackedTokenBucket
        return new TokenBucket(capacity, refillTokens, windowSeconds) {
            private final RedisBackedTokenBucket redisBucket = 
                new RedisBackedTokenBucket(key, capacity, refillTokens, windowSeconds, redisTemplate);

            @Override
            public boolean tryConsume() {
                throw new UnsupportedOperationException("Phase 1 tryConsume is deprecated. Use tryReserve, commit, and rollback.");
            }

            @Override
            public long getRemainingTokens() {
                return redisBucket.getRemainingTokens();
            }

            @Override
            public long getSecondsUntilRefill() {
                return redisBucket.getSecondsUntilRefill();
            }
        };
    }

    @Override
    public boolean tryReserve(String key, long capacity, long refillTokens, long windowSeconds, String reservationId) {
        RedisBackedTokenBucket redisBucket = new RedisBackedTokenBucket(key, capacity, refillTokens, windowSeconds, redisTemplate);
        return redisBucket.tryReserve(1, reservationId);
    }

    @Override
    public void commit(String key, String reservationId) {
        // Capacity, refillTokens, windowSeconds are not needed for commit
        RedisBackedTokenBucket redisBucket = new RedisBackedTokenBucket(key, 0, 0, 0, redisTemplate);
        redisBucket.commit(reservationId);
    }

    @Override
    public void rollback(String key, String reservationId) {
        // Capacity, refillTokens, windowSeconds are not needed for rollback script execution, but we need bucketKey
        RedisBackedTokenBucket redisBucket = new RedisBackedTokenBucket(key, 0, 0, 0, redisTemplate);
        redisBucket.rollback(1, reservationId);
    }

    @Override
    public void removeBucket(String key) {
        redisTemplate.delete("rate_limit:{" + key + "}");
    }
}
