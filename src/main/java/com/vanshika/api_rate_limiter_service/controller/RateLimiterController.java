package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import com.vanshika.api_rate_limiter_service.model.RateLimitResponse;
import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 
 * - Rate limit ENFORCEMENT → moved to RateLimitInterceptor (middleware)
 * - This controller now only exposes ADMIN/OBSERVABILITY endpoints:
 * GET /api/rate-limit/status/{key} → inspect a key's current state
 * POST /api/rate-limit/reset/{key} → reset a key's bucket (admin)
 * GET /api/rate-limit/health → health check
 *
 * These endpoints themselves are NOT protected by the interceptor
 * (which only covers /api/backend/**), because admin tools must
 * remain accessible even when rate limits are hit.
 * ─────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/rate-limit")
@Validated
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    public RateLimiterController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATUS endpoint — inspect without consuming a token
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/rate-limit/status/{key}
     *
     * Returns the current token bucket state for a given key.
     * Does NOT consume a token — safe to call for monitoring/debugging.
     *
     * Example keys: "user:42", "ip:127.0.0.1"
     */
    @GetMapping("/status/{key}")
    public ResponseEntity<ApiResponse<RateLimitResponse>> getStatus(
            @PathVariable @NotBlank(message = "Key cannot be blank") String key) {

        RateLimitStatus status = rateLimiterService.getCurrentStatus(key);
        return buildResponse(status, "Current rate limit status retrieved successfully", HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESET endpoint — admin operation to clear a key's bucket
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/rate-limit/reset/{key}
     *
     * Removes the token bucket for the given key, effectively resetting
     * that client's rate limit back to full capacity.
     *
     * Real-world use: Admin panel, support team unblocking a user,
     * automated recovery after detecting false positives.
     */
    @PostMapping("/reset/{key}")
    public ResponseEntity<ApiResponse<RateLimitResponse>> resetRateLimit(
            @PathVariable @NotBlank(message = "Key cannot be blank") String key) {

        rateLimiterService.reset(key);

        // Fetch fresh state after reset to show the restored capacity
        RateLimitStatus status = rateLimiterService.getCurrentStatus(key);
        return buildResponse(status, "Rate limit reset successfully for key: " + key, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HEALTH endpoint — always accessible, no rate limiting applied
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/rate-limit/health
     *
     * Simple liveness check.
     * In production this would be picked up by Kubernetes readiness probes,
     * load balancers, or monitoring dashboards (Grafana, Datadog, etc.).
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Rate Limiter Service is healthy", "OK"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helper — standardizes all responses from this controller
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a consistent ResponseEntity with rate limit headers and structured
     * body.
     * All endpoints in this controller funnel through here for uniformity.
     */
    private ResponseEntity<ApiResponse<RateLimitResponse>> buildResponse(
            RateLimitStatus status, String message, HttpStatus httpStatus) {

        RateLimitResponse data = new RateLimitResponse(
                status.getKey(),
                status.getRemainingTokens(),
                status.getCapacity());

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(httpStatus)
                .header("X-RateLimit-Remaining", String.valueOf(status.getRemainingTokens()))
                .header("X-RateLimit-Capacity", String.valueOf(status.getCapacity()))
                .header("X-RateLimit-Reset", String.valueOf(status.getResetSeconds()));

        // Add Retry-After only when signalling a 429 (client should wait)
        if (httpStatus == HttpStatus.TOO_MANY_REQUESTS) {
            builder.header("Retry-After", String.valueOf(status.getResetSeconds()));
        }

        return builder.body(new ApiResponse<>(httpStatus.is2xxSuccessful(), message, data));
    }
}