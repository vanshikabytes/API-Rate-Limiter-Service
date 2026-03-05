package com.vanshika.api_rate_limiter_service.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketTest {

  @Test
  void shouldConsumeTokenWhenAvailable() {
    TokenBucket bucket = new TokenBucket(2, 0);

    assertTrue(bucket.tryConsume());
    assertEquals(1, bucket.getRemainingTokens());
  }

  @Test
  void shouldNotConsumeWhenNoTokensLeft() {
    TokenBucket bucket = new TokenBucket(1, 0);

    bucket.tryConsume(); // consume first
    boolean result = bucket.tryConsume(); // second attempt

    assertFalse(result);
    assertEquals(0, bucket.getRemainingTokens());
  }

  @Test
  void shouldRefillTokensOverTime() throws InterruptedException {
    TokenBucket bucket = new TokenBucket(1, 1);

    bucket.tryConsume(); // empty bucket

    Thread.sleep(1100); // wait > 1 second

    assertTrue(bucket.tryConsume());
  }

  @Test
  void shouldAllow100RequestsAndReject101() {
    TokenBucket bucket = new TokenBucket(100, 0);

    for (int i = 0; i < 100; i++) {
      assertTrue(bucket.tryConsume(), "Request " + (i + 1) + " should be allowed");
    }

    assertFalse(bucket.tryConsume(), "Request 101 should be rejected");
  }
}