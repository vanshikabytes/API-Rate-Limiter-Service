package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import com.vanshika.api_rate_limiter_service.service.BackendService;
import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PRODUCTION-LIKE RATE LIMITER CONTROLLER (Phase-1 Optimized)
 * 
 * Flow: Client -> Rate Limiter (Middleware) -> Backend Service -> Response
 * 
 * Why Middleware?
 * 1. Security: Blocks DDoS or abusive traffic before it reaches your expensive backend.
 * 2. Performance: Backend only spends resources processing valid, authorized requests.
 */
@RestController
@RequestMapping("/api/rate-limit")
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;
    private final RateLimiterProperties properties;
    private final BackendService backendService; // Step 1: Simulated backend service

    public RateLimiterController(RateLimiterService rateLimiterService,
                                 RateLimiterProperties properties,
                                 BackendService backendService) {
        this.rateLimiterService = rateLimiterService;
        this.properties = properties;
        this.backendService = backendService;
    }

    /**
     * Main Rate Limit check endpoint.
     * Acts as a gateway. Only calls the backend if the rate limit is not exceeded.
     */
    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<Object>> checkRateLimit(@PathVariable String key) {
        
        // Step 2: Fix Token Inconsistency Bug
        // Using a single instance of TokenBucket fetched once from the service.
        // This ensures tryConsume() and getRemainingTokens() work on the SAME object state.
        TokenBucket bucket = rateLimiterService.getBucket(key);

        boolean allowed = bucket.tryConsume();
        long remaining = bucket.getRemainingTokens();

        // Resolve capacity for headers
        String type = key.contains(":") ? key.split(":")[0] : "user";
        long capacity = properties.getLimits()
                .getOrDefault(type, properties.getLimits().get("user"))
                .getCapacity();

        // Step 4: Headers are important for the client to know when they will be blocked.
        if (!allowed) {
            return ResponseEntity.status(429)
                    .header("X-RateLimit-Remaining", String.valueOf(remaining))
                    .header("X-RateLimit-Capacity", String.valueOf(capacity))
                    .body(new ApiResponse<>(false, "Rate limit exceeded. Please slow down.", null));
        }

        // Step 1: Call backend ONLY if allowed
        // This simulates a real production flow where the middleware passes the request forward.
        String backendResponse = backendService.processRequest();

        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(remaining))
                .header("X-RateLimit-Capacity", String.valueOf(capacity))
                .body(new ApiResponse<>(true, backendResponse, null));
    }

    /**
     * Step 3: Fix Reset API Design
     * Correct REST Practice: DELETE is for removing resources. 
     * Since we are "resetting" or "updating" the rate limit state, POST is more appropriate.
     */
    @PostMapping("/reset/{key}")
    public ResponseEntity<ApiResponse<Object>> resetRateLimit(@PathVariable String key) {
        rateLimiterService.reset(key);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Rate limit reset successfully for: " + key, null)
        );
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<String>> status() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Rate Limiter Service is running", "OK")
        );
    }
}