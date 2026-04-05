package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * IN-MEMORY REPOSITORY
 *
 * Implements BucketRepository (DIP - SOLID).
 * 
 * Why ConcurrentHashMap? 
 * It is thread-safe and allows multiple threads to access buckets simultaneously without full locking.
 */
@Repository
public class InMemoryBucketRepository implements BucketRepository {

    // Thread-safe map to store buckets by key
    private final ConcurrentHashMap<String, TokenBucket> bucketStore = new ConcurrentHashMap<>();

    @Override
    public TokenBucket getBucket(String key,
                                 long capacity,
                                 long refillRatePerSecond) {

        /**
         * Why computeIfAbsent?
         * It ensures ATOMIC creation. If two threads try to create a bucket for the same key
         * at the same time, only one will succeed, preventing overwriting and token loss.
         */
        return bucketStore.computeIfAbsent(
                key,
                k -> new TokenBucket(capacity, refillRatePerSecond));
    }

    @Override
    public void removeBucket(String key) {
        bucketStore.remove(key);
    }

    /**
     * Scheduled cleanup task to remove expired buckets every 60s.
     * Prevents memory leaks by clearing out inactive users.
     */
    @Scheduled(fixedRate = 60000)
    public void cleanup() {
        bucketStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}