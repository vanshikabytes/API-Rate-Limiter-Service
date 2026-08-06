package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.config.properties.TierConfig;
import com.vanshika.api_rate_limiter_service.exception.InvalidKeyException;
import com.vanshika.api_rate_limiter_service.exception.RateLimiterUnavailableException;
import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import com.vanshika.api_rate_limiter_service.model.User;
import com.vanshika.api_rate_limiter_service.repository.BucketRepository;
import com.vanshika.api_rate_limiter_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

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
     * Checks the rate limit status and reserves a token if available.
     * 
     * @param key The client key (e.g., user:alice, ip:127.0.0.1).
     * @param reservationId Unique UUID for this request.
     * @return RateLimitStatus containing remaining tokens and allowed flag.
     */
    public RateLimitStatus reserveToken(String key, String reservationId) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }

        RateLimiterProperties.RateLimitConfig config = resolveConfig(key);
        try {
            boolean allowed = repository.tryReserve(key, config.getCapacity(), config.getRefillTokens(), config.getWindowSeconds(), reservationId);
            
            TokenBucket bucket = repository.getBucket(key, config.getCapacity(), config.getRefillTokens(), config.getWindowSeconds());
            long remaining = bucket.getRemainingTokens();
            long resetSeconds = bucket.getSecondsUntilRefill();

            return new RateLimitStatus(key, remaining, config.getCapacity(), resetSeconds, allowed, false);
        } catch (Exception e) {
            log.error("Redis connection failed during reserve", e);
            // Fail Closed: Return 503 instead of 429
            throw new RateLimiterUnavailableException("Rate limiter backend is unreachable", e);
        }
    }

    public void commitToken(String key, String reservationId) {
        try {
            repository.commit(key, reservationId);
        } catch (Exception e) {
            log.error("Failed to commit reservation: {}", reservationId, e);
        }
    }

    public void rollbackToken(String key, String reservationId) {
        try {
            repository.rollback(key, reservationId);
        } catch (Exception e) {
            log.error("Failed to rollback reservation: {}", reservationId, e);
        }
    }

    /**
     * Returns the current status without consuming any tokens.
     */
    public RateLimitStatus getCurrentStatus(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }

        RateLimiterProperties.RateLimitConfig config = resolveConfig(key);
        try {
            TokenBucket bucket = repository.getBucket(key, config.getCapacity(), config.getRefillTokens(), config.getWindowSeconds());
            long remaining = bucket.getRemainingTokens();
            long resetSeconds = bucket.getSecondsUntilRefill();

            return new RateLimitStatus(key, remaining, config.getCapacity(), resetSeconds, true, false);
        } catch (Exception e) {
            throw new RateLimiterUnavailableException("Rate limiter backend is unreachable", e);
        }
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
        return repository.getBucket(key, config.getCapacity(), config.getRefillTokens(), config.getWindowSeconds());
    }

    /**
     * Resolves the limit configuration for a given key.
     */
    private RateLimiterProperties.RateLimitConfig resolveConfig(String clientKey) {
        if (clientKey.startsWith("user:")) {
            String userId = clientKey.substring(5);
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String tier = user.getTier();
                if (tier != null) {
                    java.time.LocalDateTime expiresAt = user.getTierExpiresAt();
                    if (expiresAt != null && expiresAt.isBefore(java.time.LocalDateTime.now())) {
                        RateLimiterProperties.RateLimitConfig freeConfig = tierConfig.getConfigForTier("FREE");
                        if (freeConfig != null) return freeConfig;
                    } else {
                        RateLimiterProperties.RateLimitConfig config = tierConfig.getConfigForTier(tier);
                        if (config != null) return config;
                    }
                }
            }
        }

        if (clientKey.startsWith("api-key:")) return properties.getApiKey();
        if (clientKey.startsWith("user:"))    return properties.getUser();
        if (clientKey.startsWith("backend:")) return properties.getBackend();
        return properties.getIp();
    }
}