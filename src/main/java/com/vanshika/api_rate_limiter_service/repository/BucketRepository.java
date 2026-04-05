package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.TokenBucket;

/**
 * BUCKET REPOSITORY INTERFACE
 *
 * Why this abstraction?
 * 1. Decoupling: The service layer no longer depends on a specific implementation (In-Memory).
 * 2. SOLID (Dependency Inversion): High-level modules (Service) depend on abstractions (Interface).
 * 3. Scalability: We can easily swap In-Memory with Redis in Phase-2 without changing service logic.
 */
public interface BucketRepository {

    /**
     * Fetches an existing bucket or creates a new one.
     */
    TokenBucket getBucket(String key, long capacity, long refillRate);

    /**
     * Removes a bucket (used for reset).
     */
    void removeBucket(String key);
}
