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
    long resetTime = System.currentTimeMillis() + 60000;

    String type = key.contains(":") ? key.split(":")[0] : "user";
    long capacity = properties.getLimits().getOrDefault(type, properties.getLimits().get("user")).getCapacity();

    RateLimitResponse response = new RateLimitResponse(key, remaining);

    if (!allowed) {
      return ResponseEntity.status(429)
          .header("X-RateLimit-Remaining", String.valueOf(remaining))
          .header("X-RateLimit-Capacity", String.valueOf(capacity))
          .header("X-RateLimit-Reset", String.valueOf(resetTime))
          .body(new ApiResponse<>(
              false,
              "Rate limit exceeded",
              response));
    }

    return ResponseEntity.ok()
        .header("X-RateLimit-Remaining", String.valueOf(remaining))
        .header("X-RateLimit-Capacity", String.valueOf(capacity))
        .header("X-RateLimit-Reset", String.valueOf(resetTime))
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

  @GetMapping("/status")
  public ResponseEntity<ApiResponse<String>> status() {
    return ResponseEntity.ok(
        new ApiResponse<>(
            true,
            "Rate Limiter Service is running",
            "OK"));
  }
}