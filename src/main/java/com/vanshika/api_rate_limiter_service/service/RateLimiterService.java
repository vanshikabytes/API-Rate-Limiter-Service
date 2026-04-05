package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.exception.InvalidKeyException;
import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import com.vanshika.api_rate_limiter_service.repository.BucketRepository;
import org.springframework.stereotype.Service;

/**
 * RATE LIMITER SERVICE — core business logic layer
 *
 * Why use the BucketRepository interface?
 * Dependency Inversion (SOLID) - The service layer can now work with ANY implementation, 
 * like an In-Memory bucket or a Redis-backed bucket in Phase-2.
 */
@Service
public class RateLimiterService {

    private final BucketRepository repository;
    private final RateLimiterProperties properties;

    /**
     * Dependency injection with an interface. 
     */
    public RateLimiterService(BucketRepository repository,
                              RateLimiterProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Returns the current status of a rate-limit key.
     * Centralizes logic and makes the controller "thin."
     * 
     * Why is this centralized? 
     * To ensure all logic—validation, config resolution, and bucket interaction—
     * happens in one place, following the Single Responsibility Principle.
     * 
     * @param key the rate-limit identifier
     * @return RateLimitStatus object
     */
    public RateLimitStatus getRateLimitStatus(String key) {
        // Validate key before processing
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }

        // 1. Resolve config
        RateLimiterProperties.LimitConfig config = resolveConfig(key);

        // 2. Fetch bucket
        TokenBucket bucket = repository.getBucket(key, config.getCapacity(), config.getRefillRate());

        // 3. Perform Rate Limit Check
        boolean allowed = bucket.tryConsume();
        long remaining = bucket.getRemainingTokens();
        long resetSeconds = bucket.getSecondsUntilRefill();

        // 4. Wrap everything in a status DTO and return
        return new RateLimitStatus(key, remaining, config.getCapacity(), resetSeconds, allowed);
    }

    /**
     * Retrieves the current rate limit status WITHOUT consuming a token.
     * Useful for checking state without affecting the quota.
     * 
     * @param key the rate-limit identifier
     * @return RateLimitStatus object (allowed will be true by default here)
     */
    public RateLimitStatus getCurrentStatus(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }

        RateLimiterProperties.LimitConfig config = resolveConfig(key);
        TokenBucket bucket = repository.getBucket(key, config.getCapacity(), config.getRefillRate());

        // We only fetch the state, we DO NOT call tryConsume()
        long remaining = bucket.getRemainingTokens();
        long resetSeconds = bucket.getSecondsUntilRefill();

        return new RateLimitStatus(key, remaining, config.getCapacity(), resetSeconds, true);
    }

    /**
     * Resets the rate limit for the given key.
     */
    public void reset(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }
        repository.removeBucket(key);
    }

    /**
     * Fetches the bucket directly for specialized scenarios if needed.
     * Still used by existing controller endpoints or tests.
     */
    public TokenBucket getBucket(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }

        RateLimiterProperties.LimitConfig config = resolveConfig(key);
        return repository.getBucket(key, config.getCapacity(), config.getRefillRate());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private RateLimiterProperties.LimitConfig resolveConfig(String key) {
        String type = key.contains(":") ? key.split(":")[0] : "user";
        RateLimiterProperties.LimitConfig config = properties.getLimits().get(type);

        if (config == null) {
            return properties.getLimits().get("user"); // fallback
        }
        return config;
    }
}