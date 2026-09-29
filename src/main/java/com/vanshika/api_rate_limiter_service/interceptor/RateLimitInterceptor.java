package com.vanshika.api_rate_limiter_service.interceptor;

import lombok.extern.slf4j.Slf4j;

import com.vanshika.api_rate_limiter_service.exception.RateLimitExceededException;
import com.vanshika.api_rate_limiter_service.model.AccessDecision;
import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.service.AccessControlService;
import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Intercepts incoming requests to enforce rate limits before they reach the controller.
 *
 * Phase 3 Gateway: Checks Blacklist/Whitelist FIRST using the raw userId.
 * Phase 2 Rate Limiting: Reserves token using the full prefixed key.
 */
@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimiterService rateLimiterService;
    private final AccessControlService accessControlService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService, AccessControlService accessControlService) {
        this.rateLimiterService = rateLimiterService;
        this.accessControlService = accessControlService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        // Phase 3: Resolve the RAW userId for access control lookup (matches what admin stored via PATCH)
        String rawUserId = resolveRawUserId(request);
        log.trace("Incoming request from userId: '{}', URI: {}", rawUserId, request.getRequestURI());

        // Phase 3: Access Control Gateway Check
        AccessDecision decision = accessControlService.checkAccess(rawUserId);
        log.debug("Gateway decision for '{}': {}", rawUserId, decision);

        if (decision == AccessDecision.BLOCKED) {
            log.warn("Blocked request from '{}' — returning 403 Forbidden", rawUserId);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to this API has been blocked.");
            return false; // Halts the request, bypasses Phase 2 token math completely
        }

        if (decision == AccessDecision.WHITELISTED) {
            log.info("'{}' is on the whitelist — bypassing rate limiter and allowing request", rawUserId);
            return true; // Allows request to proceed, bypasses Phase 2 token math
        }

        // Phase 2: Token Bucket Reservation (Only for NORMAL users)
        // Use the full prefixed key (e.g., "user:Client-B") for the rate limiter bucket
        String rateLimitKey = resolveRateLimitKey(request);
        String reservationId = UUID.randomUUID().toString();
        log.debug("NORMAL user '{}' going through rate limiter. Key: {}, ReservationId: {}", rawUserId, rateLimitKey, reservationId);

        request.setAttribute("RATE_LIMIT_KEY", rateLimitKey);
        request.setAttribute("RATE_LIMIT_RESERVATION_ID", reservationId);

        RateLimitStatus status = rateLimiterService.reserveToken(rateLimitKey, reservationId);
        log.debug("Token status for '{}' — remaining: {}, allowed: {}", rateLimitKey, status.getRemainingTokens(), status.isAllowed());

        response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemainingTokens()));
        response.setHeader("X-RateLimit-Capacity", String.valueOf(status.getCapacity()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(status.getResetSeconds()));

        if (rateLimitKey.startsWith("ip:") || status.isFallback()) {
            response.setHeader("X-RateLimit-Fallback", "true");
        }

        if (!status.isAllowed()) {
            log.warn("Rate limit exceeded for '{}' — {} tokens remaining, resets in {}s", rateLimitKey, status.getRemainingTokens(), status.getResetSeconds());
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

        // Only commit/rollback if a token reservation was actually made (i.e., user was NORMAL)
        if (key != null && reservationId != null) {
            if (ex == null) {
                rateLimiterService.commitToken(key, reservationId);
            } else {
                log.warn("Request failed for key {}. Rolling back reservation {}", key, reservationId);
                rateLimiterService.rollbackToken(key, reservationId);
            }
        }
    }

    /**
     * Resolves the raw identifier (e.g., "Client-B") used by Phase 3 Access Control.
     * This must match exactly what the Admin stores via the PATCH endpoint path variable.
     */
    private String resolveRawUserId(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) return apiKey;

        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank()) return userId;

        // Fallback to IP address
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    /**
     * Resolves the prefixed key (e.g., "user:Client-B") used by Phase 2 Rate Limiter buckets.
     */
    private String resolveRateLimitKey(HttpServletRequest request) {
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

