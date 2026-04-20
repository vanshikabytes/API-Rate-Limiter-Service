package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.config.properties.TierConfig;
import com.vanshika.api_rate_limiter_service.exception.InvalidKeyException;
import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import com.vanshika.api_rate_limiter_service.model.User;
import com.vanshika.api_rate_limiter_service.repository.BucketRepository;
import com.vanshika.api_rate_limiter_service.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestrates rate limiting logic.
 * 
 * This service is the heart of the rate limiter. it coordinates between
 * configuration resolution, bucket storage (in-memory or distributed),
 * and the token bucket algorithm.
 */
@Service
public class RateLimiterService {

    /**
     * Interface for token bucket storage.
     * Chosen to allow future migration to Redis without service changes.
     */
    private final BucketRepository repository;

    /**
     * Base configuration for user, IP, and backend limits.
     * Mapped from application.yaml 'rate-limiter' prefix.
     */
    private final RateLimiterProperties properties;

    /**
     * NEW: In-memory store for registered users.
     * Used to resolve user tiers before applying rate limits.
     */
    private final UserRepository userRepository;

    /**
     * NEW: Hierarchical configuration for tiered limits (free, gold, premium).
     * Provides specific limits for users who have a tier assigned.
     */
    private final TierConfig tierConfig;

    /**
     * Full constructor for dependency injection.
     */
    public RateLimiterService(BucketRepository repository,
            RateLimiterProperties properties,
            UserRepository userRepository,
            TierConfig tierConfig) {
        this.repository = repository;
        this.properties = properties;
        this.userRepository = userRepository;
        this.tierConfig = tierConfig;
    }

    /**
     * Checks the rate limit status and consumes a token if available.
     * 
     * @param key The client key (e.g., user:alice, ip:127.0.0.1).
     * @return RateLimitStatus containing remaining tokens and allowed flag.
     */
    public RateLimitStatus getRateLimitStatus(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }

        RateLimiterProperties.RateLimitConfig config = resolveConfig(key);
        // NEW: use getRefillTokens() instead of getRefillRate()
        TokenBucket bucket = repository.getBucket(key, config.getCapacity(), config.getRefillTokens(), config.getWindowSeconds());

        boolean allowed = bucket.tryConsume();
        long remaining = bucket.getRemainingTokens();
        long resetSeconds = bucket.getSecondsUntilRefill();

        return new RateLimitStatus(key, remaining, config.getCapacity(), resetSeconds, allowed);
    }

    /**
     * Returns the current status without consuming any tokens.
     */
    public RateLimitStatus getCurrentStatus(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }

        RateLimiterProperties.RateLimitConfig config = resolveConfig(key);
        // NEW: use getRefillTokens()
        TokenBucket bucket = repository.getBucket(key, config.getCapacity(), config.getRefillTokens(), config.getWindowSeconds());

        long remaining = bucket.getRemainingTokens();
        long resetSeconds = bucket.getSecondsUntilRefill();

        return new RateLimitStatus(key, remaining, config.getCapacity(), resetSeconds, true);
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

    public TokenBucket getBucket(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }

        RateLimiterProperties.RateLimitConfig config = resolveConfig(key);
        // NEW: use getRefillTokens()
        return repository.getBucket(key, config.getCapacity(), config.getRefillTokens(), config.getWindowSeconds());
    }

    /**
     * Resolves the limit configuration for a given key.
     * 
     * NEW: First checks if the user has an assigned tier (free, gold, premium).
     * If not, falls back to prefix-based static configuration (user:, backend:, ip:).
     */
    private RateLimiterProperties.RateLimitConfig resolveConfig(String clientKey) {
        // NEW: check if this key maps to a registered user with a tier
        if (clientKey.startsWith("user:")) {
            String userId = clientKey.substring(5); // NEW: extract userId from key "user:alice" → "alice"
            Optional<User> userOpt = userRepository.findById(userId); // NEW: look up User in UserRepository
            if (userOpt.isPresent()) {
                String tier = userOpt.get().getTier();
                if (tier != null) {
                    RateLimiterProperties.RateLimitConfig config = tierConfig.getConfigForTier(tier); // NEW: get tier config
                    if (config != null) {
                        return config;
                    }
                }
            }
        }

        // NEW: EXISTING FALLBACK (logic updated to use explicit fields)
        if (clientKey.startsWith("user:"))    return properties.getUser();
        if (clientKey.startsWith("backend:")) return properties.getBackend();
        return properties.getIp();
    }
}