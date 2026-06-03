package com.vanshika.api_rate_limiter_service.exception;

/**
 * Custom exception for missing business resources.
 * 
 * This allows the GlobalExceptionHandler to return a specific 404 response
 * instead of a generic 500 error when a record is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
