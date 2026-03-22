package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import com.vanshika.api_rate_limiter_service.model.RateLimitResponse;
import com.vanshika.api_rate_limiter_service.model.TimeWindow;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rate-limit")
public class RateLimiterController {

  private final RateLimiterService rateLimiterService;

  public RateLimiterController(RateLimiterService rateLimiterService) {
    this.rateLimiterService = rateLimiterService;
  }

  @GetMapping("/check")
  public ResponseEntity<ApiResponse<RateLimitResponse>> checkAdvancedRateLimit(
      @RequestParam String path,
      @RequestParam String method,
      @RequestParam String identifier) {

    RateLimitResponse response = rateLimiterService.isAllowed(path, method, identifier);
    boolean allowed = response.getRetryAfterSeconds() == 0;

    if (!allowed) {
      return ResponseEntity.status(429)
          .header("X-RateLimit-Remaining", String.valueOf(response.getRemainingTokens()))
          .header("X-RateLimit-Capacity", String.valueOf(response.getCapacity()))
          .header("Retry-After", String.valueOf(response.getRetryAfterSeconds()))
          .body(new ApiResponse<>(false, "Rate limit exceeded for path: " + path, response));
    }

    return ResponseEntity.ok()
        .header("X-RateLimit-Remaining", String.valueOf(response.getRemainingTokens()))
        .header("X-RateLimit-Capacity", String.valueOf(response.getCapacity()))
        .body(new ApiResponse<>(true, "Request allowed for path: " + path, response));
  }

  @GetMapping("/{key}")
  public ResponseEntity<ApiResponse<RateLimitResponse>> checkRateLimit(
      @PathVariable String key) {
    return checkRateLimitWithWindow(key, "minute");
  }

  @GetMapping("/{window}/{key}")
  public ResponseEntity<ApiResponse<RateLimitResponse>> checkRateLimitWithWindow(
      @PathVariable String key,
      @PathVariable String window) {

    TimeWindow timeWindow = TimeWindow.fromString(window);
    boolean allowed = rateLimiterService.isAllowed(key, timeWindow);
    TokenBucket bucket = rateLimiterService.getBucket(key, timeWindow);

    long remaining = bucket.getRemainingTokens();
    long capacity = bucket.getCapacity();
    long retryAfter = allowed ? 0 : bucket.getRetryAfterSeconds();

    RateLimitResponse response = new RateLimitResponse(key, remaining, capacity, retryAfter);

    if (!allowed) {
      return ResponseEntity.status(429)
          .header("X-RateLimit-Remaining", String.valueOf(remaining))
          .header("X-RateLimit-Capacity", String.valueOf(capacity))
          .header("Retry-After", String.valueOf(retryAfter))
          .body(new ApiResponse<>(
              false,
              "Rate limit exceeded",
              response));
    }

    return ResponseEntity.ok()
        .header("X-RateLimit-Remaining", String.valueOf(remaining))
        .header("X-RateLimit-Capacity", String.valueOf(capacity))
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
            "Rate limit reset successfully for all windows",
            null));
  }

  @GetMapping("/status")
  public ResponseEntity<ApiResponse<String>> status() {
    return ResponseEntity.ok(
        new ApiResponse<>(
            true,
            "Rate Limiter Service is running",
            "OK"));
  }
}