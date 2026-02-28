package com.vanshika.api_rate_limiter_service.model;

public class RateLimitResponse {

  private String key;
  private long remainingTokens;

  public RateLimitResponse(String key, long remainingTokens) {
    this.key = key;
    this.remainingTokens = remainingTokens;
  }

  public String getKey() {
    return key;
  }

  public long getRemainingTokens() {
    return remainingTokens;
  }
}
