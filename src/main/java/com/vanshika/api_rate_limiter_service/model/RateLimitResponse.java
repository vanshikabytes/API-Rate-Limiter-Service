package com.vanshika.api_rate_limiter_service.model;

/**
 * RATE LIMIT RESPONSE DTO
 *
 * Structured data payload returned inside ApiResponse<RateLimitResponse>.
 *
 * Keeping response structured and non-null for production standards.
 * Every response — allowed or blocked — carries this object so clients
 * always receive consistent, machine-readable metadata.
 *
 * Fields:
 *  key             — the identifier being rate-limited (e.g. "user:42")
 *  remainingTokens — how many requests the client can still make right now
 *  capacity        — the total bucket size (helps client understand the limit)
 */
public class RateLimitResponse {

    private final String key;
    private final long remainingTokens;
    private final long capacity;

    public RateLimitResponse(String key, long remainingTokens, long capacity) {
        this.key = key;
        this.remainingTokens = remainingTokens;
        this.capacity = capacity;
    }

    public String getKey() {
        return key;
    }

    public long getRemainingTokens() {
        return remainingTokens;
    }

    public long getCapacity() {
        return capacity;
    }
}
