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

        TokenBucket bucket = repository.getBucket(
                key,
                properties.getCapacity(),
                properties.getRefillRate());

        return bucket.tryConsume();
    }

    public long getRemainingTokens(String key) {

        TokenBucket bucket = repository.getBucket(
                key,
                properties.getCapacity(),
                properties.getRefillRate());

        return bucket.getRemainingTokens();
    }
}