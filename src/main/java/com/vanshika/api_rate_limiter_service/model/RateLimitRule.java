package com.vanshika.api_rate_limiter_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Defines a specific rate limiting rule for an endpoint or path pattern.
 *
 * Rules are evaluated in priority order (lowest value = highest priority).
 * This allows for specific overrides (e.g. POST /employees) to take
 * precedence over general patterns (e.g. /api/**).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitRule {

    private String id;
    private String pathPattern; // e.g. "/api/backend/employees/**"
    private String method;      // Optional: GET, POST, etc. (null = all methods)
    private int priority;       // Lower value = Higher priority (evaluated first)

    // Rate limit configuration for this specific rule
    private long capacity;
    private long refillTokens;
    private long windowSeconds;

    // Optional time-based constraints (e.g. for Peak vs Off-Peak)
    private LocalTime startTime;
    private LocalTime endTime;

    /**
     * Checks if this rule applies to the given request.
     */
    public boolean matches(String path, String requestMethod) {
        // 1. Path Pattern Match
        // Simple prefix matching as requested: /api/backend/** matches /api/backend/employees
        boolean pathMatch = false;
        if (pathPattern.endsWith("/**")) {
            String prefix = pathPattern.substring(0, pathPattern.length() - 3);
            pathMatch = path.startsWith(prefix);
        } else {
            pathMatch = path.equals(pathPattern);
        }

        if (!pathMatch) return false;

        // 2. HTTP Method Match (if specified)
        if (this.method != null && !this.method.equalsIgnoreCase(requestMethod)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the rule is currently active based on the time window.
     */
    public boolean isActiveNow() {
        if (startTime == null || endTime == null) {
            return true; // No time constraints defined
        }

        LocalTime now = LocalTime.now();

        // Handle overnight ranges (e.g. 22:00 to 06:00)
        if (startTime.isAfter(endTime)) {
            return now.isAfter(startTime) || now.isBefore(endTime);
        }

        return now.isAfter(startTime) && now.isBefore(endTime);
    }
}
