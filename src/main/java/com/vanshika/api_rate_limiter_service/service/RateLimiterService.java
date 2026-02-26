package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import com.vanshika.api_rate_limiter_service.repository.InMemoryBucketRepository;

public class RateLimiterService {

  private final InMemoryBucketRepository repository;

  private final long capacity = 10;
  private final long refillRate = 5;

  public RateLimiterService() {
    this.repository = new InMemoryBucketRepository();
  }

  public boolean isAllowed(String key) {

    TokenBucket bucket = repository.getBucket(key, capacity, refillRate);

    return bucket.tryConsume();
  }

  public long getRemainingTokens(String key) {

    TokenBucket bucket = repository.getBucket(key, capacity, refillRate);

    return bucket.getRemainingTokens();
  }
}
