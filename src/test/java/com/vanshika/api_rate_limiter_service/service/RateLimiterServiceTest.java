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
    InMemoryBucketRepository repository = new InMemoryBucketRepository();

    RateLimiterProperties properties = new RateLimiterProperties();
    properties.setCapacity(1);
    properties.setRefillRate(0);

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