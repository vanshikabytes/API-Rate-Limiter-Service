package com.vanshika.api_rate_limiter_service.exception;

import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized error handler for the entire application.
 * 
 * Using @RestControllerAdvice allows us to catch exceptions from any controller or interceptor
 * and return a consistent JSON structure to the client. This keeps our business logic
 * clean of try-catch blocks and error mapping code.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles rate limit hits (HTTP 429).
     * We include the 'Retry-After' header so clients know when to back off.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRateLimitExceeded(
            RateLimitExceededException ex) {

        log.warn("[GlobalExceptionHandler] Rate Limit Hit: {} | Remaining: {} | Reset: {}s",
                ex.getMessage(), ex.getRemainingTokens(), ex.getResetSeconds());

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

    /**
     * Handles identity/key validation errors (HTTP 400).
     */
    @ExceptionHandler(InvalidKeyException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidKey(
            InvalidKeyException ex) {

        log.info("[GlobalExceptionHandler] Bad Request: Identity validation failed: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Invalid key provided: " + ex.getMessage(), "INVALID_KEY"));
    }

    /**
     * Handles @Valid validation failures for request bodies (HTTP 400).
     * Collects all field errors so the user can fix everything in one go.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage() : "Validation failed",
                        (msg1, msg2) -> msg1 + "; " + msg2
                ));

        log.info("[GlobalExceptionHandler] Validation failed: {} error(s)", errors.size());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Request validation failed. Please correct the errors.", errors));
    }

    /**
     * Handles resource lookup failures (HTTP 404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        log.info("[GlobalExceptionHandler] Not Found: Resource lookup failed: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, ex.getMessage(), "RESOURCE_NOT_FOUND"));
    }

    /**
     * Catch-all for any unhandled exceptions (HTTP 500).
     * We don't expose stack traces or internal messages to the client for security reasons.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex) {

        log.error("[GlobalExceptionHandler] INTERNAL SERVER ERROR (500) | Message: {} | Type: {}",
                ex.getMessage(), ex.getClass().getName(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "A server-side error occurred. Our engineers have been notified.", "INTERNAL_SERVER_ERROR"));
    }
}
