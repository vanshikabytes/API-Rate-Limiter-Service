package com.vanshika.api_rate_limiter_service.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketTest {

  @Test
  void shouldConsumeTokenWhenAvailable() {
    TokenBucket bucket = new TokenBucket(2, 2, 60);

    assertTrue(bucket.tryConsume());
    assertEquals(1, bucket.getRemainingTokens());
  }

  @Test
  void shouldNotConsumeWhenNoTokensLeft() {
    TokenBucket bucket = new TokenBucket(1, 1, 60);

    bucket.tryConsume(); // consume first
    boolean result = bucket.tryConsume(); // second attempt

    assertFalse(result);
    assertEquals(0, bucket.getRemainingTokens());
  }

  @Test
  void shouldRefillTokensOverTime() throws InterruptedException {
    TokenBucket bucket = new TokenBucket(1, 1, 1); // 1 token per second

    bucket.tryConsume(); // empty bucket

    Thread.sleep(1100); // wait > 1 second

    assertTrue(bucket.tryConsume());
  }

  @Test
  void shouldAllow100RequestsAndReject101() {
    TokenBucket bucket = new TokenBucket(100, 100, 60);

    for (int i = 0; i < 100; i++) {
      assertTrue(bucket.tryConsume(), "Request " + (i + 1) + " should be allowed");
    }

    assertFalse(bucket.tryConsume(), "Request 101 should be rejected");
  }

  @Test
  void testConcurrency() throws InterruptedException {
    int iterations = 100;
    TokenBucket bucket = new TokenBucket(iterations, 0, 60);
    java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(10);
    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.atomic.AtomicInteger successCounter = new java.util.concurrent.atomic.AtomicInteger();

    for (int i = 0; i < 200; i++) {
      executor.submit(() -> {
        try {
          latch.await();
          if (bucket.tryConsume())
            successCounter.incrementAndGet();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }

    latch.countDown();
    executor.shutdown();
    executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

    assertEquals(iterations, successCounter.get(), "Exactly capacity requests should succeed in parallel");
  }
}