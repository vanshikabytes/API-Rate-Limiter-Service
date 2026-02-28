package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import com.vanshika.api_rate_limiter_service.model.RateLimitResponse;
import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rate-limit")
public class RateLimiterController {

  private final RateLimiterService rateLimiterService;

  public RateLimiterController(RateLimiterService rateLimiterService) {
    this.rateLimiterService = rateLimiterService;
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
          .body(new ApiResponse<>(
              false,
              "Rate limit exceeded",
              response));
    }

    return ResponseEntity.ok()
        .header("X-Rate-Limit-Remaining", String.valueOf(remaining))
        .body(new ApiResponse<>(
            true,
            "Request allowed",
            response));
  }
}
