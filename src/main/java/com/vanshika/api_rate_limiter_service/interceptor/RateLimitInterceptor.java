package com.vanshika.api_rate_limiter_service.interceptor;

import com.vanshika.api_rate_limiter_service.exception.RateLimitExceededException;
import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.model.RedisBackedTokenBucket;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepts incoming requests to enforce rate limits before they reach the
 * controller.
 * 
 * We throw a custom exception on limit hits instead of writing to the response
 * here.
 * This keeps the logic clean and lets the GlobalExceptionHandler handle the
 * JSON formatting.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        // Identify client: API Key → User ID → IP Address (Priority Order)
        String apiKey = request.getHeader("X-API-Key");
        String userId = request.getHeader("X-User-Id");

        String key;
        if (apiKey != null && !apiKey.isBlank()) {
            key = "api-key:" + apiKey;
        } else if (userId != null && !userId.isBlank()) {
            key = "user:" + userId;
        } else {
            String xff = request.getHeader("X-Forwarded-For");
            String ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
            key = "ip:" + ip;
        }

        // Check token availability
        RateLimitStatus status = rateLimiterService.getRateLimitStatus(key);

        // Always include limit metadata in headers for client-side visibility
        response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemainingTokens()));
        response.setHeader("X-RateLimit-Capacity",  String.valueOf(status.getCapacity()));
        response.setHeader("X-RateLimit-Reset",     String.valueOf(status.getResetSeconds()));

        // Phase 2: If Redis failed and the bucket fell back to fail-open, signal it
        TokenBucket bucket = rateLimiterService.getBucket(key);
        if (bucket instanceof RedisBackedTokenBucket redisBucket && redisBucket.isFallback()) {
            response.setHeader("X-RateLimit-Fallback", "true");
        }

        if (!status.isAllowed()) {
            // Throwing an exception here triggers the global error handler
            // This ensures a consistent error response structure across the app
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Try again in " + status.getResetSeconds() + " second(s).",
                    status.getRemainingTokens(),
                    status.getCapacity(),
                    status.getResetSeconds());
        }

        return true; // Token consumed, allow request to proceed
    }
}
