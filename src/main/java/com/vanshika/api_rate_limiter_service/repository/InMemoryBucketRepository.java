package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBucketRepository {

  private final RateLimiterProperties properties;
  private final ConcurrentHashMap<String, TokenBucket> bucketStore = new ConcurrentHashMap<>();

  public InMemoryBucketRepository(RateLimiterProperties properties) {
    this.properties = properties;
  }

  public TokenBucket getBucket(String key,
      long capacity,
      long refillRate,
      long windowDurationSeconds) {

    return bucketStore.computeIfAbsent(
        key,
        k -> new TokenBucket(capacity, refillRate, windowDurationSeconds));
  }

  public void removeBucket(String key) {
    bucketStore.remove(key);
  }

  @Scheduled(fixedDelayString = "${rate-limiter.cleanup-interval-ms:60000}")
  public void cleanup() {
    bucketStore.entrySet().removeIf(entry -> entry.getValue().isExpired(properties.getCleanupTtlSeconds()));
  }
}