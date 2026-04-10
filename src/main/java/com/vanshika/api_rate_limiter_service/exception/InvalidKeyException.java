package com.vanshika.api_rate_limiter_service.exception;

/**
 * Custom exception for identity validation failures.
 * 
 * Thrown when a rate limit key (like user ID or IP) is missing or malformed.
 * This ensures the client receives an HTTP 400 Bad Request instead of a server crash.
 */
public class InvalidKeyException extends RuntimeException {

    public InvalidKeyException(String message) {
        super(message);
    }
}
