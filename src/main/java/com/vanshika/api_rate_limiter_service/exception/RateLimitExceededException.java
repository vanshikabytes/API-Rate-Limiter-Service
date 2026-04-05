package com.vanshika.api_rate_limiter_service.exception;

/**
 * RATE LIMIT EXCEEDED EXCEPTION — Step 6: Custom Exception for HTTP 429
 *
 * ─────────────────────────────────────────────────────────────
 * Why a custom exception for rate limiting?
 * ─────────────────────────────────────────────────────────────
 * Using a specific, typed exception for rate limiting provides:
 *   ✔ Intent: Developers reading the code immediately know what went wrong.
 *   ✔ Payload: We can attach critical metadata (remainingTokens, capacity, resetSeconds)
 *              that the GlobalExceptionHandler can use to build consistent headers.
 *   ✔ Precision: Allows the ExceptionHandler to map this EXACT error to HTTP 429,
 *              separate from other 4xx client errors like 404 (Not Found).
 *
 * ─────────────────────────────────────────────────────────────
 * Real-world relevance:
 * ─────────────────────────────────────────────────────────────
 * Large-scale APIs (GitHub, Twitter, Stripe) use custom exception hierarchies
 * to ensure that infrastructure-level errors (like being throttled) are
 * distinct from business-logic errors (like an invalid user ID).
 */
public class RateLimitExceededException extends RuntimeException {

    private final long remainingTokens;
    private final long capacity;
    private final long resetSeconds;

    /**
     * Constructs a new RateLimitExceededException.
     *
     * @param message         Human-readable explanation of why the request was blocked.
     * @param remainingTokens How many tokens are left (usually 0).
     * @param capacity        Maximum allowed tokens for this user.
     * @param resetSeconds    Wait time in seconds until the refill.
     */
    public RateLimitExceededException(String message, long remainingTokens, long capacity, long resetSeconds) {
        super(message);
        this.remainingTokens = remainingTokens;
        this.capacity = capacity;
        this.resetSeconds = resetSeconds;
    }

    public long getRemainingTokens() {
        return remainingTokens;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getResetSeconds() {
        return resetSeconds;
    }
}
