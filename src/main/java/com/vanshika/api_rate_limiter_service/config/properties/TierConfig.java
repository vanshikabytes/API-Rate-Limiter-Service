package com.vanshika.api_rate_limiter_service.config.properties;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration class for the four user tiers: FREE, PRO, ENTERPRISE, UNLIMITED.
 *
 * Maps the 'rate-limiter.tiers' section of application.yaml into Java objects.
 * Tier names are matched case-insensitively so "free", "FREE", and "Free" all resolve.
 */
@ConfigurationProperties(prefix = "rate-limiter.tiers")
public class TierConfig {

    /**
     * FREE tier — most restrictive. Default for new or expired users.
     * Capacity: 3 req/min.
     */
    private RateLimiterProperties.RateLimitConfig FREE;

    /**
     * PRO tier — standard paid tier.
     * Capacity: 50 req/min.
     */
    private RateLimiterProperties.RateLimitConfig PRO;

    /**
     * ENTERPRISE tier — high-volume tier for business clients.
     * Capacity: 200 req/min.
     */
    private RateLimiterProperties.RateLimitConfig ENTERPRISE;

    /**
     * UNLIMITED tier — effectively no rate limiting (10 000 req/min).
     * Intended for internal services or premium partners.
     */
    private RateLimiterProperties.RateLimitConfig UNLIMITED;

    /**
     * Resolves the RateLimitConfig for the given tier name (case-insensitive).
     *
     * @param tier The tier identifier (FREE, PRO, ENTERPRISE, UNLIMITED).
     * @return The matching RateLimitConfig, or null if unrecognized.
     */
    public RateLimiterProperties.RateLimitConfig getConfigForTier(String tier) {
        if (tier == null) return null;
        return switch (tier.toUpperCase()) {
            case "FREE"       -> FREE;
            case "PRO"        -> PRO;
            case "ENTERPRISE" -> ENTERPRISE;
            case "UNLIMITED"  -> UNLIMITED;
            default           -> null;
        };
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public RateLimiterProperties.RateLimitConfig getFREE() { return FREE; }
    public void setFREE(RateLimiterProperties.RateLimitConfig FREE) { this.FREE = FREE; }

    public RateLimiterProperties.RateLimitConfig getPRO() { return PRO; }
    public void setPRO(RateLimiterProperties.RateLimitConfig PRO) { this.PRO = PRO; }

    public RateLimiterProperties.RateLimitConfig getENTERPRISE() { return ENTERPRISE; }
    public void setENTERPRISE(RateLimiterProperties.RateLimitConfig ENTERPRISE) { this.ENTERPRISE = ENTERPRISE; }

    public RateLimiterProperties.RateLimitConfig getUNLIMITED() { return UNLIMITED; }
    public void setUNLIMITED(RateLimiterProperties.RateLimitConfig UNLIMITED) { this.UNLIMITED = UNLIMITED; }
}
