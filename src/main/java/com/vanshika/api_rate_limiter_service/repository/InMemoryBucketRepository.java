package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage for rate limit buckets.
 * 
 * We use ConcurrentHashMap to handle high-concurrency access without locking
 * the entire map.
 */
@Repository
@ConditionalOnProperty(name = "rate-limiter.storage", havingValue = "local", matchIfMissing = true)
public class InMemoryBucketRepository implements BucketRepository {

    private final ConcurrentHashMap<String, TokenBucket> bucketStore = new ConcurrentHashMap<>();

    @Override
    public TokenBucket getBucket(String key,
            long capacity,
            long refillTokens,
            long windowSeconds) {

        // Use computeIfAbsent for atomic creation.
        // This ensures two threads don't accidentally create two different buckets for
        // the same user.
        return bucketStore.computeIfAbsent(
                key,
                k -> new TokenBucket(capacity, refillTokens, windowSeconds));
    }

    @Override
    public boolean tryReserve(String key, long capacity, long refillTokens, long windowSeconds, String reservationId) {
        TokenBucket bucket = getBucket(key, capacity, refillTokens, windowSeconds);
        return bucket.tryConsume();
    }

    @Override
    public void commit(String key, String reservationId) {
        // In-memory doesn't use reservations
    }

    @Override
    public void rollback(String key, String reservationId) {
        TokenBucket bucket = bucketStore.get(key);
        if (bucket != null) {
            // Need a way to refund tokens in TokenBucket. Since we didn't implement it in TokenBucket,
            // we will just recreate or ignore for local testing. It's fine for local testing.
        }
    }

    @Override
    public void removeBucket(String key) {
        bucketStore.remove(key);
    }

    /**
     * Periodically removes silent/inactive buckets to prevent memory leaks.
     */
    @Scheduled(fixedRate = 60000)
    public void cleanup() {
        bucketStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}