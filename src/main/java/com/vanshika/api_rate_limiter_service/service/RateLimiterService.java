package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.model.TimeWindow;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import com.vanshika.api_rate_limiter_service.repository.InMemoryBucketRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

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
        return isAllowed(key, TimeWindow.MINUTE);
    }

    public boolean isAllowed(String key, TimeWindow window) {
        RateLimiterProperties.LimitConfig config = resolveConfig(key, window);
        TokenBucket bucket = repository.getBucket(
                generateBucketKey(key, window),
                config.getCapacity(),
                config.getRefillRate(),
                window.getSeconds());

        return bucket.tryConsume();
    }

    public long getRemainingTokens(String key) {
        return getRemainingTokens(key, TimeWindow.MINUTE);
    }

    public long getRemainingTokens(String key, TimeWindow window) {
        RateLimiterProperties.LimitConfig config = resolveConfig(key, window);
        TokenBucket bucket = repository.getBucket(
                generateBucketKey(key, window),
                config.getCapacity(),
                config.getRefillRate(),
                window.getSeconds());

        return bucket.getRemainingTokens();
    }

    public TokenBucket getBucket(String key, TimeWindow window) {
        RateLimiterProperties.LimitConfig config = resolveConfig(key, window);
        return repository.getBucket(
                generateBucketKey(key, window),
                config.getCapacity(),
                config.getRefillRate(),
                window.getSeconds());
    }

    private String generateBucketKey(String key, TimeWindow window) {
        String type = resolveType(key);
        return type + ":" + window.name().toLowerCase() + ":" + key;
    }

    private String resolveType(String key) {
        return key.contains(":") ? key.split(":")[0] : "user";
    }

    private RateLimiterProperties.LimitConfig resolveConfig(String key, TimeWindow window) {
        String type = resolveType(key);
        Map<String, RateLimiterProperties.LimitConfig> windowLimits = properties.getLimits().get(type);

        if (windowLimits == null) {
            windowLimits = properties.getLimits().get("user");
        }

        RateLimiterProperties.LimitConfig config = windowLimits.get(window.name().toLowerCase());
        if (config == null) {
            // Fallback to minute if specific window not found
            config = windowLimits.get("minute");
        }
        return config;
    }

    public void reset(String key) {
        // Reset all possible windows for the key
        for (TimeWindow window : TimeWindow.values()) {
            repository.removeBucket(generateBucketKey(key, window));
        }
    }
}