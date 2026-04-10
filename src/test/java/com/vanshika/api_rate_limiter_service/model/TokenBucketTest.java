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
    // 1 token capacity, refill 1 token every 1 second
    TokenBucket bucket = new TokenBucket(1, 1, 1);

    bucket.tryConsume(); // empty bucket

    Thread.sleep(1100); // wait > 1 second

    assertTrue(bucket.tryConsume());
  }

  @Test
  void shouldAllowPartialRefill() throws InterruptedException {
    // 10 tokens capacity, refill 10 tokens every 1 second (10 tokens/sec)
    TokenBucket bucket = new TokenBucket(10, 10, 1);
    
    // Exhaust tokens
    for(int i=0; i<10; i++) bucket.tryConsume();
    assertEquals(0, bucket.getRemainingTokens());

    // Wait 500ms -> should have ~5 tokens
    Thread.sleep(550); 
    
    long remaining = bucket.getRemainingTokens();
    assertTrue(remaining >= 5, "Should have refilled at least 5 tokens, but got " + remaining);
    assertTrue(remaining < 10, "Should not have fully refilled yet");
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
  void shouldHandleConcurrentRequests() throws InterruptedException {
      int capacity = 1000;
      int threadCount = 50;
      TokenBucket bucket = new TokenBucket(capacity, 0, 60);

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