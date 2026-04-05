package com.vanshika.api_rate_limiter_service.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * BACKEND RESPONSE DTO — Step 5: Clean Response Structure
 *
 * ─────────────────────────────────────────────────────────────
 * Why use a DTO instead of a raw String or Map?
 * ─────────────────────────────────────────────────────────────
 * In production systems, returning raw strings or untyped Maps
 * causes problems:
 *   - No compile-time type safety → silent bugs
 *   - No documentation → impossible to generate OpenAPI/Swagger docs
 *   - No consistency → every endpoint can return a slightly different shape
 *   - Harder to test → assertions must compare raw JSON strings
 *
 * DTOs solve all of this:
 *   - Strongly typed → compile-time safety
 *   - Self-documenting → IDE shows field names and types
 *   - Consistent → all endpoints use the same structure
 *   - Testable → compare field-by-field, not string comparison
 *
 * ─────────────────────────────────────────────────────────────
 * Real-world relevance:
 * ─────────────────────────────────────────────────────────────
 * Every production API (Stripe, GitHub, AWS) returns structured
 * responses with a unique request ID. This allows:
 *   - Distributed tracing → find a specific request in logs across services
 *   - Debugging → a client can file a bug report with the requestId
 *   - Auditing → attach a requestId to every log line in the backend
 *
 * requestId: Unique ID for THIS specific request — critical for tracing.
 * timestamp: When the response was generated — helpful for debugging.
 * message:   Human-readable summary of what happened.
 * data:      Generic payload — can be any object (user, list, map, etc.)
 */
public class BackendResponse {

    /**
     * Unique identifier for this request.
     * Set once on construction — immutable per response.
     *
     * Real-world: Stripe calls this "request_id", AWS calls it "RequestId".
     * Both log it on their side so any support ticket can trace back instantly.
     */
    private final String requestId;

    /**
     * Server-side timestamp when this response was created (ISO-8601 UTC).
     * Helps clients detect clock drift or stale cached responses.
     */
    private final Instant timestamp;

    /**
     * Human-readable message describing the result.
     * Ex: "Data retrieved successfully", "User found", "Processing complete".
     */
    private final String message;

    /**
     * Generic payload — the actual business data.
     * Using Object here so one DTO can wrap any response type
     * (UserResponse, a Map, a List, etc.) without creating
     * one DTO per endpoint. Jackson serializes it correctly regardless.
     */
    private final Object data;

    // ─────────────────────────────────────────────────────────────
    // Constructor — auto-generates requestId and timestamp
    // ─────────────────────────────────────────────────────────────

    /**
     * Creates a new BackendResponse.
     * requestId and timestamp are auto-generated — callers should not
     * pass those in, ensuring each response gets a truly unique ID.
     *
     * @param message Human-readable result message
     * @param data    The actual response payload
     */
    public BackendResponse(String message, Object data) {
        // UUID v4 → cryptographically random, globally unique
        this.requestId = UUID.randomUUID().toString();
        // Instant.now() → current UTC time (no timezone ambiguity)
        this.timestamp = Instant.now();
        this.message   = message;
        this.data      = data;
    }

    // ─────────────────────────────────────────────────────────────
    // Getters — required for Jackson to serialize fields to JSON
    // ─────────────────────────────────────────────────────────────

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
