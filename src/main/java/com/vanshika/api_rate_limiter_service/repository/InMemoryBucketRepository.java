package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IN-MEMORY REPOSITORY
 * 
 * Uses ConcurrentHashMap for thread-safe storage of token buckets.
 * This is Phase-1 implementation (No Redis).
 */
@Repository 
public class InMemoryBucketRepository {

    // Thread-safe map to store buckets by key
    private final ConcurrentHashMap<String, TokenBucket> bucketStore = new ConcurrentHashMap<>();

    /**
     * Fetches an existing bucket or creates a new one if it doesn't exist.
     * computeIfAbsent is atomic, ensuring thread-safety.
     */
    public TokenBucket getBucket(String key,
                                 long capacity,
                                 long refillRatePerSecond) {

        return bucketStore.computeIfAbsent(
                key,
                k -> new TokenBucket(capacity, refillRatePerSecond));
    }

    /**
     * Removes a bucket from storage (used for Reset API).
     */
    public void removeBucket(String key) {
        bucketStore.remove(key);
    }

    /**
     * Scheduled cleanup task to remove expired buckets from memory every 60s.
     * Prevents memory leaks by clearing out inactive users.
     */
    @Scheduled(fixedRate = 60000)
    public void cleanup() {
        bucketStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}