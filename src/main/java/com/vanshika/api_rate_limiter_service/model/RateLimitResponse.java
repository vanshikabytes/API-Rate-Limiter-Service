package com.vanshika.api_rate_limiter_service.model;

public class RateLimitResponse {

  private String key;
  private long remainingTokens;
  private long capacity;
  private long retryAfterSeconds;

  public RateLimitResponse(String key, long remainingTokens, long capacity, long retryAfterSeconds) {
    this.key = key;
    this.remainingTokens = remainingTokens;
    this.capacity = capacity;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public String getKey() { return key; }
  public long getRemainingTokens() { return remainingTokens; }
  public long getCapacity() { return capacity; }
  public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
