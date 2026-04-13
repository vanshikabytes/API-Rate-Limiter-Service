package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.exception.InvalidKeyException;
import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import com.vanshika.api_rate_limiter_service.repository.BucketRepository;
import org.springframework.stereotype.Service;

/**
 * Orchestrates rate limiting logic.
 * 
 * We use the BucketRepository interface to decouple the service from the actual
 * storage.
 * This makes it easy to swap the in-memory store for Redis later without
 * changing this code.
 */
@Service
public class RateLimiterService {

    private final BucketRepository repository;
    private final RateLimiterProperties properties;

    public RateLimiterService(BucketRepository repository,
            RateLimiterProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Checks the rate limit status using a custom configuration (override).
     * Used by the Rules Engine to apply specific endpoint/time-based limits.
     */
    public RateLimitStatus getRateLimitStatusWithOverride(
            String key,
            long capacity,
            long refillTokens,
            long windowSeconds) {

        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }

        return repository.consumeToken(key, capacity, refillTokens, windowSeconds);
    }

    /**
     * Checks the rate limit status and consumes a token if available.
     * Centralizing this here keeps our interceptors and controllers thin.
     */
    public RateLimitStatus getRateLimitStatus(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }

        RateLimiterProperties.LimitConfig config = resolveConfig(key);
        return repository.consumeToken(key, config.getCapacity(), config.getRefillRate(), config.getWindowSeconds());
    }

    /**
     * Returns the current status without consuming any tokens.
     */
    public RateLimitStatus getCurrentStatus(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }

        RateLimiterProperties.LimitConfig config = resolveConfig(key);
        return repository.getStatus(key, config.getCapacity(), config.getRefillRate(), config.getWindowSeconds());
    }

    /**
     * Resets the rate limit for a specific key by removing its bucket.
     */
    public void reset(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }
        repository.removeBucket(key);
    }

    // Helper to decide which limit config (user vs ip) to apply based on the key
    // prefix
    private RateLimiterProperties.LimitConfig resolveConfig(String key) {
        if (key.startsWith("user:")) {
            String userId = key.substring(5); // Remove "user:" prefix
            if (properties.getUsers() != null && properties.getUsers().containsKey(userId)) {
                return properties.getUsers().get(userId);
            }
        }

        // Fallback to default config
        return properties.getLimits().get("default");
    }
}