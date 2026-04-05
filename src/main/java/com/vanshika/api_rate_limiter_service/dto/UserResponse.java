package com.vanshika.api_rate_limiter_service.dto;

import java.time.Instant;

/**
 * USER RESPONSE DTO — Step 5: Typed Response for /users/{id}
 *
 * ─────────────────────────────────────────────────────────────
 * Why a separate UserResponse DTO instead of a generic Map?
 * ─────────────────────────────────────────────────────────────
 * Maps (Map<String, Object>) are tempting for quick "flexible" responses
 * but cause real problems in production:
 *
 *   - No contract: Map keys are just strings — easy to misspell, impossible to refactor safely.
 *   - No type safety: A field typed "Object" can silently change from String to Integer.
 *   - No documentation: OpenAPI / Swagger cannot infer schema from a raw Map.
 *   - No IDE support: Devs cannot autocomplete or navigate to field definitions.
 *
 * With a typed DTO:
 *   ✔ Field names are enforced at compile time
 *   ✔ Jackson serializes fields in a deterministic, documented order
 *   ✔ Adding a field is a safe, traceable change across the codebase
 *   ✔ Easier to write unit tests (compare .getId(), not map.get("id"))
 *
 * ─────────────────────────────────────────────────────────────
 * Real-world relevance:
 * ─────────────────────────────────────────────────────────────
 * In microservice architectures, a "User" response DTO would typically be
 * generated from a Protobuf or OpenAPI spec and shared as a library
 * between the user-service, notification-service, and gateway.
 * This ensures all services agree on the shape of a "User" at compile time.
 *
 * id:        Unique identifier for the user (e.g., from the database primary key)
 * name:      Display name returned from user profile
 * createdAt: When the user was created — useful for "member since" features
 */
public class UserResponse {

    /**
     * The unique user identifier.
     * In production, this would be a UUID from the database,
     * or an auto-incremented Long — we use String here for flexibility.
     */
    private final String id;

    /**
     * The user's display name.
     * In production, could be "firstName lastName" assembled from the User entity.
     */
    private final String name;

    /**
     * The user's email address.
     * Included to simulate a realistic user profile payload.
     */
    private final String email;

    /**
     * When the user account was created (ISO-8601 UTC).
     * In production, this comes from the user's database row `created_at` column.
     * Returning an Instant (not a formatted string) lets the client format it
     * according to their own locale / timezone — a best practice for APIs.
     */
    private final Instant createdAt;

    // ─────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────

    /**
     * Constructs a fully populated UserResponse.
     * All fields are set at construction and are immutable — this ensures
     * the DTO is thread-safe and cannot be partially initialized.
     *
     * @param id        User's unique identifier
     * @param name      User's display name
     * @param email     User's email address
     * @param createdAt Timestamp when the user was created
     */
    public UserResponse(String id, String name, String email, Instant createdAt) {
        this.id        = id;
        this.name      = name;
        this.email     = email;
        this.createdAt = createdAt;
    }

    // ─────────────────────────────────────────────────────────────
    // Getters — required for Jackson to serialize to JSON
    // ─────────────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
