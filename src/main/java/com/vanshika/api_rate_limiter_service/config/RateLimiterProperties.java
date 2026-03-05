package com.vanshika.api_rate_limiter_service.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

  private Map<String, LimitConfig> limits;

  public Map<String, LimitConfig> getLimits() {
    return limits;
  }

  public void setLimits(Map<String, LimitConfig> limits) {
    this.limits = limits;
  }

  public static class LimitConfig {

    private long capacity;
    private long refillRate;
    private long windowSeconds;

    public long getCapacity() {
      return capacity;
    }

    public void setCapacity(long capacity) {
      this.capacity = capacity;
    }

    public long getRefillRate() {
      return refillRate;
    }

    public void setRefillRate(long refillRate) {
      this.refillRate = refillRate;
    }

    public long getWindowSeconds() {
      return windowSeconds;
    }

    public void setWindowSeconds(long windowSeconds) {
      this.windowSeconds = windowSeconds;
    }
  }
}