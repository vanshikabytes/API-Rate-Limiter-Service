package com.vanshika.api_rate_limiter_service.interceptor;

import com.vanshika.api_rate_limiter_service.exception.RateLimitExceededException;
import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * RATE LIMIT INTERCEPTOR — Step 6: Refactored Error Handling
 *
 * ─────────────────────────────────────────────────────
 * What has changed in Step 6?
 * ─────────────────────────────────────────────────────
 * In Step 4/5, this interceptor manually wrote JSON to the response body
 * and returned 'false' to block the request.
 *
 * In Step 6, we follow the "Clean Architecture" pattern:
 *   1. If rate limit is hit, we THROW a RateLimitExceededException.
 *   2. We let the @RestControllerAdvice (GlobalExceptionHandler) catch it.
 *   3. This separates "Traffic Enforcement" logic from "Response Formatting" logic.
 *
 * Flow:
 *   Client Request → Interceptor (Throws Exception) → GlobalExceptionHandler (Formats JSON) → Client
 *
 * Benefits:
 *   ✔ Cleaner Code: No manual PrintWriter or JSON string building here.
 *   ✔ Consistency: All errors (4xx, 500) now flow through one central handler.
 *   ✔ Separation of Concerns: This class only cares about the rate-limiting algorithm.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Runs BEFORE the controller method is invoked.
     *
     * @return true  → let the request continue to the controller
     * @throws RateLimitExceededException → if the limit is reached
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws Exception {

        // 1. Extract Identity
        String userId = request.getHeader("X-User-Id");
        String key;

        if (userId != null && !userId.isBlank()) {
            key = "user:" + userId;
        } else {
            key = "ip:" + request.getRemoteAddr();
        }

        // 2. Consume Token & Get Status
        RateLimitStatus status = rateLimiterService.getRateLimitStatus(key);

        // 3. Set informational headers (Standard visibility for clients)
        response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemainingTokens()));
        response.setHeader("X-RateLimit-Capacity",  String.valueOf(status.getCapacity()));
        response.setHeader("X-RateLimit-Reset",     String.valueOf(status.getResetSeconds()));

        // 4. Handle Blocked Request
        if (!status.isAllowed()) {
            // Instead of manually writing 429 response, we throw.
            // GlobalExceptionHandler will catch this and add headers like Retry-After.
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Try again in " + status.getResetSeconds() + " second(s).",
                    status.getRemainingTokens(),
                    status.getCapacity(),
                    status.getResetSeconds()
            );
        }

        return true; // ALLOW — continue to controller
    }
}
