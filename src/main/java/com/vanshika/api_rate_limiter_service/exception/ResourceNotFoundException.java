package com.vanshika.api_rate_limiter_service.exception;

/**
 * RESOURCE NOT FOUND EXCEPTION — Step 6: Custom Exception for HTTP 404
 *
 * ─────────────────────────────────────────────────────────────
 * Why a custom exception for Not Found?
 * ─────────────────────────────────────────────────────────────
 * Standard Spring RestTemplate / WebClient can throw this, but
 * having a custom, domain-specific exception (e.g. UserNotFound)
 * is the Clean Architecture way to signal that a business resource
 * could not be located in the database.
 *
 * Benefits:
 *   ✔ Contract: We map it to HTTP 404, not just a generic error.
 *   ✔ Traceability: Controllers can catch this and return specific messages.
 *   ✔ Standard: Follows HTTP RFC standards for mapping missing items.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException.
     *
     * @param message Specific explanation of what's missing (e.g., "User not found with ID: 123").
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
