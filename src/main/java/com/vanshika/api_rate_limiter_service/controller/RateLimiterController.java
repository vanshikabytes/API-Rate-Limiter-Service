package com.vanshika.api_rate_limiter_service.controller;

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
  public ResponseEntity<?> checkRateLimit(@PathVariable String key) {

    boolean allowed = rateLimiterService.isAllowed(key);

    Map<String, Object> response = new HashMap<>();
    response.put("key", key);
    response.put("allowed", allowed);
    response.put("remainingTokens",
        rateLimiterService.getRemainingTokens(key));

    if (!allowed) {
      return ResponseEntity.status(429).body(response);
    }

    return ResponseEntity.ok(response);
  }
}
