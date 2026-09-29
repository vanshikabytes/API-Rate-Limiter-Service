package com.vanshika.api_rate_limiter_service.exception;

public class AccessControlException extends RuntimeException {
    public AccessControlException(String message) {
        super(message);
    }
}
