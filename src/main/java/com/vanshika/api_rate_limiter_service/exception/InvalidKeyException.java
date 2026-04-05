package com.vanshika.api_rate_limiter_service.exception;

/**
 * INVALID KEY EXCEPTION — Step 6: Custom Exception for HTTP 400
 *
 * ─────────────────────────────────────────────────────────────
 * Why a custom exception for key validation?
 * ─────────────────────────────────────────────────────────────
 * In a rate limiter, "key" is the foundation of identity (X-User-Id, IP).
 * If the key is null, blank, or formatted incorrectly, it is a
 * caller-side error — the server doesn't know who to limit.
 *
 * Using InvalidKeyException instead of IllegalArgumentException:
 *   ✔ Contract: We explicitly communicate that identity validation failed.
 *   ✔ Routing: The ExceptionHandler can route this directly to HTTP 400.
 *   ✔ Logging: We can set specific log levels or triggers for common key errors.
 */
public class InvalidKeyException extends RuntimeException {

    /**
     * Constructs a new InvalidKeyException.
     *
     * @param message Specific explanation of what's wrong with the key.
     */
    public InvalidKeyException(String message) {
        super(message);
    }
}
