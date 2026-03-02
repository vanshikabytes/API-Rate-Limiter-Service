package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import com.vanshika.api_rate_limiter_service.model.RateLimitResponse;
import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rate-limit")
public class RateLimiterController {

  private final RateLimiterService rateLimiterService;
  private final RateLimiterProperties properties;

  public RateLimiterController(RateLimiterService rateLimiterService,
      RateLimiterProperties properties) {
    this.rateLimiterService = rateLimiterService;
    this.properties = properties;
  }

  @GetMapping("/{key}")
  public ResponseEntity<ApiResponse<RateLimitResponse>> checkRateLimit(
      @PathVariable String key) {

    boolean allowed = rateLimiterService.isAllowed(key);
    long remaining = rateLimiterService.getRemainingTokens(key);

    RateLimitResponse response = new RateLimitResponse(key, remaining);

    if (!allowed) {
      return ResponseEntity.status(429)
          .header("X-Rate-Limit-Remaining", String.valueOf(remaining))
          .header("X-Rate-Limit-Capacity", String.valueOf(properties.getCapacity()))
          .header("X-Rate-Limit-Refill-Rate", String.valueOf(properties.getRefillRate()))
          .body(new ApiResponse<>(
              false,
              "Rate limit exceeded",
              response));
    }

    return ResponseEntity.ok()
        .header("X-Rate-Limit-Remaining", String.valueOf(remaining))
        .header("X-Rate-Limit-Capacity", String.valueOf(properties.getCapacity()))
        .header("X-Rate-Limit-Refill-Rate", String.valueOf(properties.getRefillRate()))
        .body(new ApiResponse<>(
            true,
            "Request allowed",
            response));
  }

  @DeleteMapping("/{key}")
  public ResponseEntity<ApiResponse<Object>> resetRateLimit(
      @PathVariable String key) {

    rateLimiterService.reset(key);

    return ResponseEntity.ok(
        new ApiResponse<>(
            true,
            "Rate limit reset successfully",
            null));
  }
}