package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.model.TimeWindow;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import com.vanshika.api_rate_limiter_service.model.RateRule;
import com.vanshika.api_rate_limiter_service.model.RateLimitResponse;
import com.vanshika.api_rate_limiter_service.repository.InMemoryBucketRepository;
import com.vanshika.api_rate_limiter_service.repository.RedisBucketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);
    private final InMemoryBucketRepository repository;
    private final RedisBucketRepository redisRepository;
    private final RuleEngineService ruleEngine;
    private final RateLimiterProperties properties;

    public RateLimiterService(InMemoryBucketRepository repository,
            RedisBucketRepository redisRepository,
            RuleEngineService ruleEngine,
            RateLimiterProperties properties) {
        this.repository = repository;
        this.redisRepository = redisRepository;
        this.ruleEngine = ruleEngine;
        this.properties = properties;
    }

    public boolean isAllowed(String key) {
        return isAllowed(key, TimeWindow.MINUTE);
    }

    public RateLimitResponse isAllowed(String path, String method, String identifier) {
        Optional<RateRule> rule = ruleEngine.match(path, method);
        if (rule.isPresent()) {
            RateRule r = rule.get();
            String bucketKey = "rule:" + r.getName() + ":" + identifier;
            try {
                List<Long> result = redisRepository.tryConsume(
                        bucketKey,
                        r.getCapacity(),
                        r.getRefillRate(),
                        r.getWindow().getSeconds(),
                        1);
                
                boolean allowed = result != null && result.get(0) == 1;
                long remaining = result != null ? result.get(1) : 0;
                long retryAfter = result != null ? result.get(2) : 0;

                return new RateLimitResponse(bucketKey, remaining, r.getCapacity(), allowed ? 0 : retryAfter);

            } catch (Exception e) {
                logger.warn("Redis is unavailable for Rule {}, falling back to In-Memory", r.getName(), e);
                TokenBucket backupBucket = repository.getBucket(
                        bucketKey,
                        r.getCapacity(),
                        r.getRefillRate(),
                        r.getWindow().getSeconds());
                
                boolean allowed = backupBucket.tryConsume();
                return new RateLimitResponse(bucketKey, backupBucket.getRemainingTokens(), r.getCapacity(), allowed ? 0 : backupBucket.getRetryAfterSeconds());
            }
        }

        // Fallback to default user minute limit if no specific rule matches
        String key = "user:" + identifier;
        RateLimiterProperties.LimitConfig config = resolveConfig(key, TimeWindow.MINUTE);
        
        try {
            List<Long> result = redisRepository.tryConsume(
                generateBucketKey(key, TimeWindow.MINUTE),
                config.getCapacity(),
                config.getRefillRate(),
                TimeWindow.MINUTE.getSeconds(),
                1);
            
            boolean allowed = result != null && result.get(0) == 1;
            long remaining = result != null ? result.get(1) : 0;
            long retryAfter = result != null ? result.get(2) : 0;

            return new RateLimitResponse(key, remaining, config.getCapacity(), allowed ? 0 : retryAfter);
        } catch (Exception e) {
            TokenBucket bucket = repository.getBucket(
                generateBucketKey(key, TimeWindow.MINUTE),
                config.getCapacity(),
                config.getRefillRate(),
                TimeWindow.MINUTE.getSeconds());
            boolean allowed = bucket.tryConsume();
            return new RateLimitResponse(key, bucket.getRemainingTokens(), config.getCapacity(), allowed ? 0 : bucket.getRetryAfterSeconds());
        }
    }

    public boolean isAllowed(String key, TimeWindow window) {
        RateLimiterProperties.LimitConfig config = resolveConfig(key, window);
        
        try {
            // ===== PHASE 2 (Redis) =====
            List<Long> result = redisRepository.tryConsume(
                    generateBucketKey(key, window),
                    config.getCapacity(),
                    config.getRefillRate(),
                    window.getSeconds(),
                    1);
            
            return result != null && result.get(0) == 1;
        } catch (Exception e) {
            logger.warn("Redis is unavailable for key {}, falling back to Phase 1 (In-Memory)", key, e);
            // ===== PHASE 1 (In-Memory) Fallback =====
            TokenBucket bucket = repository.getBucket(
                    generateBucketKey(key, window),
                    config.getCapacity(),
                    config.getRefillRate(),
                    window.getSeconds());
            return bucket.tryConsume();
        }
    }

    public long getRemainingTokens(String key) {
        return getRemainingTokens(key, TimeWindow.MINUTE);
    }

    public long getRemainingTokens(String key, TimeWindow window) {
        RateLimiterProperties.LimitConfig config = resolveConfig(key, window);
        
        try {
            // ===== PHASE 2 (Redis) =====
            List<Long> result = redisRepository.tryConsume(
                    generateBucketKey(key, window),
                    config.getCapacity(),
                    config.getRefillRate(),
                    window.getSeconds(),
                    0); // 0 means just checking
            
            return result != null ? result.get(1) : 0;
        } catch (Exception e) {
            logger.warn("Redis is unavailable for key {}, falling back to Phase 1 (In-Memory)", key, e);
            // ===== PHASE 1 (In-Memory) Fallback =====
            TokenBucket bucket = repository.getBucket(
                    generateBucketKey(key, window),
                    config.getCapacity(),
                    config.getRefillRate(),
                    window.getSeconds());
            return bucket.getRemainingTokens();
        }
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
            redisRepository.removeBucket(generateBucketKey(key, window));
        }
    }
}