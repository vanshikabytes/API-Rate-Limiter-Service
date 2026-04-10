package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.TokenBucket;

/**
 * Interface for token bucket storage.
 * 
 * This abstraction allows us to switch from in-memory storage to a distributed
 * store like Redis without having to modify our service logic.
 */
public interface BucketRepository {

    /**
     * Retrieves an existing bucket or initializes a new one for the given key.
     */
    TokenBucket getBucket(String key, long capacity, long windowSeconds);

    /**
     * Clears a bucket entry, effectively resetting the rate limit for that key.
     */

    void removeBucket(String key);
}
