package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.repository.InMemoryBucketRepository;
import com.vanshika.api_rate_limiter_service.repository.RedisBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class RateLimiterServiceTest {

  private RateLimiterService service;
  private RedisBucketRepository redisRepository;

  @BeforeEach
  void setup() {
    RateLimiterProperties properties = new RateLimiterProperties();
    RateLimiterProperties.LimitConfig userConfig = new RateLimiterProperties.LimitConfig();
    userConfig.setCapacity(1);
    userConfig.setRefillRate(0);

    properties.setLimits(java.util.Map.of("user", java.util.Map.of("minute", userConfig)));

    InMemoryBucketRepository repository = new InMemoryBucketRepository(properties);
    redisRepository = Mockito.mock(RedisBucketRepository.class);
    RuleEngineService ruleEngine = Mockito.mock(RuleEngineService.class);
    service = new RateLimiterService(repository, redisRepository, ruleEngine, properties);
  }

  @Test
  void shouldBlockAfterCapacityReached() {
    when(redisRepository.tryConsume(anyString(), anyLong(), anyLong(), anyLong(), eq(1)))
        .thenReturn(List.of(1L, 0L))
        .thenReturn(List.of(0L, 0L));

    assertTrue(service.isAllowed("user1"));
    assertFalse(service.isAllowed("user1"));
  }

  @Test
  void shouldResetBucket() {
    when(redisRepository.tryConsume(anyString(), anyLong(), anyLong(), anyLong(), eq(1)))
        .thenReturn(List.of(1L, 0L));

    service.isAllowed("user1");
    service.reset("user1");

    assertTrue(service.isAllowed("user1"));
  }
}