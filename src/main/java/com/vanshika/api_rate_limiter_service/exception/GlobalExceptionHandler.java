package com.vanshika.api_rate_limiter_service.exception;

import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * GLOBAL EXCEPTION HANDLER — Step 6: Production-Grade Error Management
 *
 * ─────────────────────────────────────────────────────────────
 * What is @RestControllerAdvice?
 * ─────────────────────────────────────────────────────────────
 * A specialized @Component for building global, cross-cutting error handlers
 * for REST components. It can catch exceptions from:
 *   ✔ Controller methods
 *   ✔ Interceptors (if using a ControllerAdvice with an ExceptionHandler)
 *   ✔ Service methods
 *
 * Benefits:
 *   ✔ Consistency: EVERY endpoint in the backend follows the same JSON shape for errors.
 *   ✔ Cleanliness: Controllers only have "happy-path" code. Error mapping is centralized here.
 *   ✔ Decoupling: The service layer can throw custom exceptions without knowing about HTTP.
 *
 * ─────────────────────────────────────────────────────────────
 * Why HTTP status codes matter?
 * ─────────────────────────────────────────────────────────────
 * APIs are meant for both humans and machines.
 *
 *   ✔ Status 400: "You (client) sent bad data" (e.g., empty key)
 *   ✔ Status 404: "You (client) asked for something that doesn't exist" (e.g., missing user)
 *   ✔ Status 429: "You (client) are calling too fast" (e.g., rate limit hit)
 *   ✔ Status 500: "I (server) crashed" (e.g., unexpected error)
 *
 * This allows client libraries (e.g., Axios, Fetch, OkHttp) to automatically
 * retry, back-off, or redirect based on the status code without parsing the JSON body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. Rate Limit Exceeded → HTTP 429
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Handles RateLimitExceededException thrown by the RateLimitInterceptor.
     * Maps to HTTP 429 Too Many Requests (RFC 6585).
     *
     * We MUST include headers like Retry-After so that compliant clients
     * know exactly when it is safe to try again.
     *
     * @param ex the rate limit exception
     * @return 429 response Entity
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRateLimitExceeded(
            RateLimitExceededException ex) {

        log.warn("[GlobalExceptionHandler] Rate Limit Hit: {} | Remaining: {} | Reset: {}s",
                ex.getMessage(), ex.getRemainingTokens(), ex.getResetSeconds());

        // Standard structure for 429 payload
        Map<String, Object> data = Map.of(
                "remainingTokens", ex.getRemainingTokens(),
                "capacity",        ex.getCapacity(),
                "resetSeconds",    ex.getResetSeconds()
        );

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getResetSeconds()))
                .header("X-RateLimit-Remaining", String.valueOf(ex.getRemainingTokens()))
                .header("X-RateLimit-Capacity", String.valueOf(ex.getCapacity()))
                .body(new ApiResponse<>(false, ex.getMessage(), data));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. Invalid Key → HTTP 400
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Handles InvalidKeyException thrown by the service layer.
     * Maps to HTTP 400 Bad Request.
     *
     * @param ex the key validation exception
     * @return 400 response Entity
     */
    @ExceptionHandler(InvalidKeyException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidKey(
            InvalidKeyException ex) {

        log.info("[GlobalExceptionHandler] Bad Request: Identity validation failed: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Invalid key provided: " + ex.getMessage(), "INVALID_KEY"));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. Resource Not Found → HTTP 404
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Handles ResourceNotFoundException thrown by the backend service.
     * Maps to HTTP 404 Not Found.
     *
     * @param ex basic mapping exception
     * @return 404 response Entity
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        log.info("[GlobalExceptionHandler] Not Found: Resource lookup failed: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, ex.getMessage(), "RESOURCE_NOT_FOUND"));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. Catch-All Generic Exception → HTTP 500
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Handles all unexpected exceptions.
     * Maps to HTTP 500 Internal Server Error.
     *
     * ⚠️ WARNING: In production, we NEVER return the actual Exception message
     * as it could leak sensitive internal details (SQL queries, stack traces, etc.).
     * We return a generic message to the client but log the full trace for the VPC.
     *
     * @param ex the unexpected crash
     * @return 500 response Entity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex) {

        log.error("[GlobalExceptionHandler] INTERNAL SERVER ERROR (500) | Message: {} | Type: {}",
                ex.getMessage(), ex.getClass().getName(), ex);

        // Production mapping: Hide the exception details from the outside world.
        // During dev, you might set a profile to include 'ex.getMessage()' or use a debugger.
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "A server-side error occurred. Our engineers have been notified.", "INTERNAL_SERVER_ERROR"));
    }
}
