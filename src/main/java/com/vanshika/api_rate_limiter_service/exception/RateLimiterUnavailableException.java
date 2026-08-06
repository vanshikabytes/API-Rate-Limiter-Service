package com.vanshika.api_rate_limiter_service.exception;

public class RateLimiterUnavailableException extends RuntimeException {
    public RateLimiterUnavailableException(String message) {
        super(message);
    }

    public RateLimiterUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
