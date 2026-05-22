package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.config.properties.TierConfig;
import com.vanshika.api_rate_limiter_service.exception.InvalidKeyException;
import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import com.vanshika.api_rate_limiter_service.model.User;
import com.vanshika.api_rate_limiter_service.repository.BucketRepository;
import com.vanshika.api_rate_limiter_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Orchestrates rate limiting logic.
 *
 * This service is the heart of the rate limiter. It coordinates between
 * configuration resolution, bucket storage (in-memory or distributed),
 * and the token bucket algorithm.
 *
 * Phase-2 additions:
 *  - Tier expiration: if a user's tier has a past tierExpiresAt, they are
 *    automatically treated as FREE without any manual downgrade step.
 *  - Storage abstraction: BucketRepository is injected — switching from
 *    in-memory to Redis requires only a config flag change, not code changes.
 */
@Service
public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);

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
     * In-memory store for registered users.
     * Used to resolve user tiers before applying rate limits.
     */
    private final UserRepository userRepository;

    /**
     * Hierarchical configuration for tiered limits (FREE, PRO, ENTERPRISE, UNLIMITED).
     * Provides specific limits for users who have a tier assigned.
     */
    private final TierConfig tierConfig;

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
        TokenBucket bucket = repository.getBucket(
                key, config.getCapacity(), config.getRefillTokens(), config.getWindowSeconds());

        boolean allowed    = bucket.tryConsume();
        long    remaining  = bucket.getRemainingTokens();
        long    resetSecs  = bucket.getSecondsUntilRefill();

        return new RateLimitStatus(key, remaining, config.getCapacity(), resetSecs, allowed);
    }

    /**
     * Returns the current status without consuming any tokens.
     */
    public RateLimitStatus getCurrentStatus(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidKeyException("Key cannot be null or empty");
        }

        RateLimiterProperties.RateLimitConfig config = resolveConfig(key);
        TokenBucket bucket = repository.getBucket(
                key, config.getCapacity(), config.getRefillTokens(), config.getWindowSeconds());

        long remaining = bucket.getRemainingTokens();
        long resetSecs = bucket.getSecondsUntilRefill();

        return new RateLimitStatus(key, remaining, config.getCapacity(), resetSecs, true);
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
        return repository.getBucket(
                key, config.getCapacity(), config.getRefillTokens(), config.getWindowSeconds());
    }

    /**
     * Resolves the limit configuration for a given client key.
     *
     * Resolution order:
     *  1. If the key maps to a registered user with an assigned tier:
     *       a. Check if tierExpiresAt is set and in the past.
     *          If expired → log a warning, fall back to FREE tier config.
     *       b. Otherwise → return the tier-specific config from TierConfig.
     *  2. Prefix-based static fallback:
     *       api-key: → apiKey config
     *       user:    → user config
     *       backend: → backend config
     *       default  → ip config
     */
    private RateLimiterProperties.RateLimitConfig resolveConfig(String clientKey) {

        // ── Step 1: User-tier resolution ─────────────────────────────────────
        if (clientKey.startsWith("user:")) {
            String userId = clientKey.substring(5); // "user:alice" → "alice"
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String tier = user.getTier();

                if (tier != null) {
                    // Phase-2: Automatic tier expiration
                    LocalDateTime expiresAt = user.getTierExpiresAt();
                    if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
                        logger.warn("Tier '{}' for user '{}' expired at {}. " +
                                    "Auto-downgrading to FREE tier.",
                                tier, userId, expiresAt);
                        // Re-evaluated every request — no mutation of User object.
                        RateLimiterProperties.RateLimitConfig freeConfig =
                                tierConfig.getConfigForTier("FREE");
                        if (freeConfig != null) return freeConfig;
                    } else {
                        RateLimiterProperties.RateLimitConfig tierCfg =
                                tierConfig.getConfigForTier(tier);
                        if (tierCfg != null) return tierCfg;
                    }
                }
            }
        }

        // ── Step 2: Prefix-based static fallback ─────────────────────────────
        if (clientKey.startsWith("api-key:")) return properties.getApiKey();
        if (clientKey.startsWith("user:"))    return properties.getUser();
        if (clientKey.startsWith("backend:")) return properties.getBackend();
        return properties.getIp();
    }
}