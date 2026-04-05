package com.vanshika.api_rate_limiter_service.model;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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

  /**
   * CONCURRENCY TEST — Task 3
   *
   * Validates that TokenBucket is thread-safe.
   * Multiple threads consume tokens simultaneously.
   * We expect exactly 'capacity' tokens to be consumed successfully,
   * no matter how many threads try or in what order.
   */
  @Test
  void shouldHandleConcurrentRequests() throws InterruptedException {
      int capacity = 1000;
      int threadCount = 50;
      TokenBucket bucket = new TokenBucket(capacity, 0);

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(1);
      AtomicInteger successfulConsumes = new AtomicInteger(0);

      for (int i = 0; i < capacity + 500; i++) {
          executor.submit(() -> {
              try {
                  latch.await(); // wait for all threads to be ready
                  if (bucket.tryConsume()) {
                      successfulConsumes.incrementAndGet();
                  }
              } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
              }
          });
      }

      latch.countDown(); // Start all threads at once
      executor.shutdown();
      while (!executor.isTerminated()) {
          Thread.sleep(10);
      }

      // Assert that exactly 'capacity' tokens were consumed, not more (no race condition)
      assertEquals(capacity, successfulConsumes.get(),
          "Bucket allowed more than capacity tokens under high concurrent load!");
      assertEquals(0, bucket.getRemainingTokens(),
          "Bucket should be empty after all possible tokens were consumed.");
  }
}