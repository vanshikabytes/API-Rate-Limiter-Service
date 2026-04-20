package com.vanshika.api_rate_limiter_service.config.properties;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration class for different user tiers (free, gold, premium).
 * 
 * This class maps the 'rate-limiter.tiers' section of application.yaml into Java objects.
 * It provides a central way to manage different service levels for users.
 */
@ConfigurationProperties(prefix = "rate-limiter.tiers")
public class TierConfig {

    /**
     * Configuration for the 'free' tier.
     * Chosen to be the most restrictive tier for basic users.
     */
    private RateLimiterProperties.RateLimitConfig free;

    /**
     * Configuration for the 'gold' tier.
     * A middle-ground tier for regular active users.
     */
    private RateLimiterProperties.RateLimitConfig gold;

    /**
     * Configuration for the 'premium' tier.
     * The most generous tier for power users or paid subscribers.
     */
    private RateLimiterProperties.RateLimitConfig premium;

    /**
     * Resolves the RateLimitConfig based on the tier name.
     * 
     * @param tier The tier identifier (free, gold, premium).
     * @return The matching RateLimitConfig, or null if unrecognized.
     */
    public RateLimiterProperties.RateLimitConfig getConfigForTier(String tier) {
        if (tier == null) return null;
        return switch (tier.toLowerCase()) {
            case "free" -> free;
            case "gold" -> gold;
            case "premium" -> premium;
            default -> null;
        };
    }

    public RateLimiterProperties.RateLimitConfig getFree() {
        return free;
    }

    public void setFree(RateLimiterProperties.RateLimitConfig free) {
        this.free = free;
    }

    public RateLimiterProperties.RateLimitConfig getGold() {
        return gold;
    }

    public void setGold(RateLimiterProperties.RateLimitConfig gold) {
        this.gold = gold;
    }

    public RateLimiterProperties.RateLimitConfig getPremium() {
        return premium;
    }

    public void setPremium(RateLimiterProperties.RateLimitConfig premium) {
        this.premium = premium;
    }
}
