package com.vanshika.api_rate_limiter_service.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard data transfer object for all backend responses.
 * 
 * Every response includes a unique request ID and a timestamp, which are 
 * essential for distributed tracing and debugging in production environments.
 */
public class BackendResponse {

    // Unique identifier used to trace this specific request in logs.
    private final String requestId;

    // Server-side timestamp of when the response was generated.
    private final Instant timestamp;

    // Human-readable summary of the operation.
    private final String message;

    // The actual payload (User object, List, Map, etc.).
    private final Object data;

    /**
     * Constructor that automatically generates a unique requestId and timestamp.
     */
    public BackendResponse(String message, Object data) {
        this.requestId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.message   = message;
        this.data      = data;
    }

    // Getters for Jackson serialization.
    public String getRequestId() {
        return requestId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
