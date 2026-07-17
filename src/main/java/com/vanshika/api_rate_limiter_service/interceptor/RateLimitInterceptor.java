package com.vanshika.api_rate_limiter_service.interceptor;

import com.vanshika.api_rate_limiter_service.exception.RateLimitExceededException;
import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Intercepts incoming requests to enforce rate limits before they reach the
 * controller.
 * 
 * Phase 1: Generates UUID and reserves the token.
 * Phase 3: Commits or rolls back the reservation upon completion.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String key = resolveKey(request);
        String reservationId = UUID.randomUUID().toString();
        
        // Store for Phase 3
        request.setAttribute("RATE_LIMIT_KEY", key);
        request.setAttribute("RATE_LIMIT_RESERVATION_ID", reservationId);

        // Phase 1: Reserve Token
        RateLimitStatus status = rateLimiterService.reserveToken(key, reservationId);

        response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemainingTokens()));
        response.setHeader("X-RateLimit-Capacity", String.valueOf(status.getCapacity()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(status.getResetSeconds()));

        if (key.startsWith("ip:") || status.isFallback()) {
            response.setHeader("X-RateLimit-Fallback", "true");
        }

        if (!status.isAllowed()) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Try again in " + status.getResetSeconds() + " second(s).",
                    status.getRemainingTokens(),
                    status.getCapacity(),
                    status.getResetSeconds());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String key = (String) request.getAttribute("RATE_LIMIT_KEY");
        String reservationId = (String) request.getAttribute("RATE_LIMIT_RESERVATION_ID");

        if (key != null && reservationId != null) {
            if (ex == null) {
                // Phase 3A: Success - Commit the reservation (token remains consumed)
                rateLimiterService.commitToken(key, reservationId);
            } else {
                // Phase 3B: Server Failure - Rollback the reservation (refund the token)
                log.warn("Request failed for key {}. Rolling back reservation {}", key, reservationId);
                rateLimiterService.rollbackToken(key, reservationId);
            }
        }
    }

    private String resolveKey(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) return "api-key:" + apiKey;

        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank()) return "user:" + userId;

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = request.getRemoteAddr();
        }
        return "ip:" + ipAddress;
    }
}
