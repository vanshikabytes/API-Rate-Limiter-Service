package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.TokenBucket;
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