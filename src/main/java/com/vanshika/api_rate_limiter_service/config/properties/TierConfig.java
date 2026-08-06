package com.vanshika.api_rate_limiter_service.config.properties;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration class for different user tiers (FREE, PRO, ENTERPRISE, UNLIMITED).
 * 
 * This class maps the 'rate-limiter.tiers' section of application.yaml into Java objects.
 * It provides a central way to manage different service levels for users.
 */
@Component
@ConfigurationProperties(prefix = "rate-limiter.tiers")
public class TierConfig {

    private RateLimiterProperties.RateLimitConfig free = new RateLimiterProperties.RateLimitConfig();
    private RateLimiterProperties.RateLimitConfig pro = new RateLimiterProperties.RateLimitConfig();
    private RateLimiterProperties.RateLimitConfig enterprise = new RateLimiterProperties.RateLimitConfig();
    private RateLimiterProperties.RateLimitConfig unlimited = new RateLimiterProperties.RateLimitConfig();

    public RateLimiterProperties.RateLimitConfig getConfigForTier(String tier) {
        if (tier == null) return null;
        return switch (tier.toUpperCase()) {
            case "FREE" -> free;
            case "PRO" -> pro;
            case "ENTERPRISE" -> enterprise;
            case "UNLIMITED" -> unlimited;
            default -> null;
        };
    }

    public RateLimiterProperties.RateLimitConfig getFree() {
        return free;
    }

    public void setFree(RateLimiterProperties.RateLimitConfig free) {
        this.free = free;
    }

    public RateLimiterProperties.RateLimitConfig getPro() {
        return pro;
    }

    public void setPro(RateLimiterProperties.RateLimitConfig pro) {
        this.pro = pro;
    }

    public RateLimiterProperties.RateLimitConfig getEnterprise() {
        return enterprise;
    }

    public void setEnterprise(RateLimiterProperties.RateLimitConfig enterprise) {
        this.enterprise = enterprise;
    }

    public RateLimiterProperties.RateLimitConfig getUnlimited() {
        return unlimited;
    }

    public void setUnlimited(RateLimiterProperties.RateLimitConfig unlimited) {
        this.unlimited = unlimited;
    }
}
