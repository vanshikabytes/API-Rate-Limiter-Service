package com.vanshika.api_rate_limiter_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

  private long capacity;
  private long refillRate;

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
}