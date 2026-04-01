package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import com.vanshika.api_rate_limiter_service.repository.InMemoryBucketRepository;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final InMemoryBucketRepository repository;
    private final RateLimiterProperties properties;

    public RateLimiterService(InMemoryBucketRepository repository,
            RateLimiterProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public boolean isAllowed(String key) {
        RateLimiterProperties.LimitConfig config = resolveConfig(key);
        TokenBucket bucket = repository.getBucket(
                key,
                config.getCapacity(),
                config.getRefillRate());

        return bucket.tryConsume();
    }

    public long getRemainingTokens(String key) {
        RateLimiterProperties.LimitConfig config = resolveConfig(key);
        TokenBucket bucket = repository.getBucket(
                key,
                config.getCapacity(),
                config.getRefillRate());

        return bucket.getRemainingTokens();
    }

    public TokenBucket getBucket(String key) {
        // Step 2: Fix Token Inconsistency Bug
        // Fetching config once and then returning the bucket.
        // This ensures the controller works on the SAME TokenBucket instance.
        RateLimiterProperties.LimitConfig config = resolveConfig(key);
        return repository.getBucket(
                key,
                config.getCapacity(),
                config.getRefillRate());
    }

    private RateLimiterProperties.LimitConfig resolveConfig(String key) {
        String type = key.contains(":") ? key.split(":")[0] : "user";// Extract type of key
        RateLimiterProperties.LimitConfig config = properties.getLimits().get(type);// fetches corresponding limits.

        if (config == null) {
            // Default to 'user' limits if type not found
            return properties.getLimits().get("user");
        }
        return config;
    }

    public void reset(String key) {
        repository.removeBucket(key);
    }
}