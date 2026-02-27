package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import org.springframework.stereotype.Repository;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBucketRepository {

  private final ConcurrentHashMap<String, TokenBucket> bucketStore = new ConcurrentHashMap<>();

  public TokenBucket getBucket(String key,
      long capacity,
      long refillRatePerSecond) {

    return bucketStore.computeIfAbsent(
        key,
        k -> new TokenBucket(capacity, refillRatePerSecond));
  }
}