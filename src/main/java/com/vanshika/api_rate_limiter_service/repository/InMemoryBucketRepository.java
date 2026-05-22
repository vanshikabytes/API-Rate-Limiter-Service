package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage for rate limit buckets.
 *
 * Active only when rate-limiter.storage=memory (or when the property is absent).
 * Uses ConcurrentHashMap to handle high-concurrency access without locking the
 * entire map.
 *
 * To switch to Redis distributed mode, set rate-limiter.storage=redis in
 * application.yaml — no code changes required.
 */
@Repository
@ConditionalOnProperty(
    name         = "rate-limiter.storage",
    havingValue  = "memory",
    matchIfMissing = true   // default when property is absent
)
public class InMemoryBucketRepository implements BucketRepository {

    private final ConcurrentHashMap<String, TokenBucket> bucketStore = new ConcurrentHashMap<>();

    @Override
    public TokenBucket getBucket(String key,
            long capacity,
            long refillTokens,
            long windowSeconds) {

        // computeIfAbsent is atomic — two concurrent threads cannot create
        // two different buckets for the same user.
        return bucketStore.computeIfAbsent(
                key,
                k -> new TokenBucket(capacity, refillTokens, windowSeconds));
    }

    @Override
    public void removeBucket(String key) {
        bucketStore.remove(key);
    }

    /**
     * Periodically removes inactive buckets to prevent memory leaks.
     * Runs every 60 seconds; isExpired() checks for 5 minutes of inactivity.
     */
    @Scheduled(fixedRate = 60_000)
    public void cleanup() {
        bucketStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}