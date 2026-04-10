package com.vanshika.api_rate_limiter_service.exception;

/**
 * Custom exception thrown when a client hits their rate limit.
 * 
 * By using a specific exception, we can attach usage metadata that the 
 * GlobalExceptionHandler needs to populate standard 429 response headers.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long remainingTokens;
    private final long capacity;
    private final long resetSeconds;

    /**
     * @param message         Explanation for the client.
     * @param remainingTokens Current token count (usually 0).
     * @param capacity        Client's maximum bucket size.
     * @param resetSeconds    Wait time before the next successful request.
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
