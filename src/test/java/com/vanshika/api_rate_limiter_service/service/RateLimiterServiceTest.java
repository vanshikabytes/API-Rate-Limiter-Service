package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.repository.InMemoryBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

  private RateLimiterService service;

  @BeforeEach
  void setup() {
    RateLimiterProperties properties = new RateLimiterProperties();
    RateLimiterProperties.LimitConfig userConfig = new RateLimiterProperties.LimitConfig();
    userConfig.setCapacity(1);
    userConfig.setRefillRate(0);

    properties.setLimits(java.util.Map.of("user", java.util.Map.of("minute", userConfig)));

    InMemoryBucketRepository repository = new InMemoryBucketRepository(properties);
    service = new RateLimiterService(repository, properties);
  }

  @Test
  void shouldBlockAfterCapacityReached() {
    assertTrue(service.isAllowed("user1"));
    assertFalse(service.isAllowed("user1"));
  }

  @Test
  void shouldResetBucket() {
    service.isAllowed("user1");
    service.reset("user1");

    assertTrue(service.isAllowed("user1"));
  }
}