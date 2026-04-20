package com.vanshika.api_rate_limiter_service.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Maps configuration from application.yml to Java objects.
 * 
 * This allows us to change rate limits dynamically without re-compiling the code.
 * The prefix is 'rate-limiter'.
 */
@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

  private Map<String, RateLimitConfig> limits;
  private Map<String, RateLimitConfig> users;
  private RateLimitConfig user; // NEW: Added for prefix-based fallback
  private RateLimitConfig ip; // NEW: Added for prefix-based fallback
  private RateLimitConfig backend; // NEW: Added for prefix-based fallback

  public Map<String, RateLimitConfig> getLimits() {
    return limits;
  }

  public void setLimits(Map<String, RateLimitConfig> limits) {
    this.limits = limits;
  }

  public Map<String, RateLimitConfig> getUsers() {
    return users;
  }

  public void setUsers(Map<String, RateLimitConfig> users) {
    this.users = users;
  }

  public RateLimitConfig getUser() { // NEW: Getter for prefix-based fallback
    return user;
  }

  public void setUser(RateLimitConfig user) { // NEW: Setter for prefix-based fallback
    this.user = user;
  }

  public RateLimitConfig getIp() { // NEW: Getter for prefix-based fallback
    return ip;
  }

  public void setIp(RateLimitConfig ip) { // NEW: Setter for prefix-based fallback
    this.ip = ip;
  }

  public RateLimitConfig getBackend() { // NEW: Getter for prefix-based fallback
    return backend;
  }

  public void setBackend(RateLimitConfig backend) { // NEW: Setter for prefix-based fallback
    this.backend = backend;
  }

  /**
   * Data Transfer Object for rate limit configuration.
   * Holds the capacity, refill speed, and window duration.
   */
  public static class RateLimitConfig { // NEW: Renamed from LimitConfig

    private long capacity;
    private long refillTokens; // NEW: Renamed from refillRate
    private long windowSeconds;

    public long getCapacity() {
      return capacity;
    }

    public void setCapacity(long capacity) {
      this.capacity = capacity;
    }

    public long getRefillTokens() { // NEW: Renamed getter
      return refillTokens;
    }

    public void setRefillTokens(long refillTokens) { // NEW: Renamed setter
      this.refillTokens = refillTokens;
    }

    public long getWindowSeconds() {
      return windowSeconds;
    }

    public void setWindowSeconds(long windowSeconds) {
      this.windowSeconds = windowSeconds;
    }
  }
}