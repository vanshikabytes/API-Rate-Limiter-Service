package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.dto.BackendResponse;
import com.vanshika.api_rate_limiter_service.dto.UserResponse;
import com.vanshika.api_rate_limiter_service.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * BACKEND SERVICE — Step 5: Realistic Business Logic Layer
 *
 * ─────────────────────────────────────────────────────────────
 * Why is BackendService separate from the controller?
 * ─────────────────────────────────────────────────────────────
 * This follows the standard Spring layered architecture:
 *
 *   Controller  → receives HTTP request, delegates to service, returns response
 *   Service     → contains all business logic, orchestrates data access
 *   Repository  → data access only (database queries, cache reads)
 *
 * Benefits of this separation:
 *   ✔ Testability:    Service logic can be unit-tested without mocking HTTP or Spring context
 *   ✔ Reusability:    Multiple controllers (REST, GraphQL, gRPC) can call the same service
 *   ✔ Maintainability: Business logic is in one place, not scattered across controllers
 *   ✔ Decoupling:     Logic changes don't require touching controller-layer code
 *
 * ─────────────────────────────────────────────────────────────
 * Why is BackendService separate from RateLimiterService?
 * ─────────────────────────────────────────────────────────────
 * The rate limiter is cross-cutting infrastructure — it applies to ALL endpoints.
 * Business logic is domain-specific — it applies to a specific feature.
 *
 * Mixing them would violate the Single Responsibility Principle:
 *   "A class should have one, and only one, reason to change."
 *
 * RateLimiterService changes when the rate-limiting STRATEGY changes.
 * BackendService changes when the BUSINESS REQUIREMENTS change.
 * These are completely independent concerns.
 *
 * ─────────────────────────────────────────────────────────────
 * Real-world note:
 * ─────────────────────────────────────────────────────────────
 * In a real microservice, each service method would:
 *   - Query a database via a JPA Repository
 *   - Call downstream services via RestTemplate or WebClient
 *   - Publish events to a message queue (Kafka, RabbitMQ)
 *   - Write to a cache (Redis, Caffeine)
 *
 * We simulate these with Thread.sleep() and hardcoded data so the
 * rate-limiter's behavior is realistic and demonstrable in a self-contained app.
 */
@Service
public class BackendService {

    // ─────────────────────────────────────────────────────────────
    // Logger — production-grade logging with SLF4J + Logback
    // ─────────────────────────────────────────────────────────────
    // Why SLF4J? It's a facade — the actual logging library (Logback,
    // Log4j2) can be swapped without changing any code in this class.
    // Spring Boot auto-configures Logback by default.
    private static final Logger log = LoggerFactory.getLogger(BackendService.class);

    // ─────────────────────────────────────────────────────────────
    // Simulated latency constant
    // ─────────────────────────────────────────────────────────────
    // Real production backends have network I/O, database roundtrips,
    // and cache lookups that add ~20-100ms per request.
    // We simulate this to make rate-limiting demos feel authentic.
    private static final long SIMULATED_LATENCY_MS = 50;

    // ─────────────────────────────────────────────────────────────
    // Public Service Methods
    // ─────────────────────────────────────────────────────────────

    /**
     * Retrieves generic application data.
     *
     * Real-world equivalent: fetching a dashboard summary from multiple sources —
     * active users count, system health, recent events — assembled into one response.
     *
     * @return BackendResponse wrapping a data summary map
     */
    public BackendResponse getData() {
        log.info("[BackendService] getData() called — simulating data fetch");

        // In production: query multiple repositories, aggregate, and return.
        // Here we return a representative static structure.
        Map<String, Object> payload = Map.of(
                "source",      "in-memory-store",
                "recordCount", 42,
                "status",      "healthy",
                "environment", "demo",
                "note",        "In production, this data comes from a real database or cache."
        );

        log.info("[BackendService] getData() — returning {} records from source", 42);

        // Wrap in DTO with auto-generated requestId and timestamp
        return new BackendResponse("Data retrieved successfully", payload);
    }

    /**
     * Fetches simulated user data for the given user ID.
     *
     * Real-world equivalent: a UserService that queries a PostgreSQL users table
     * via JPA, runs a cache lookup in Redis first, and maps the entity to a DTO.
     *
     * @param id The user's unique identifier (from URL path variable)
     * @return BackendResponse wrapping a UserResponse DTO
     */
    public BackendResponse getUserById(String id) {
        log.info("[BackendService] getUserById() called — fetching user with id={}", id);

        // Simulate database latency
        simulateLatency();

        // SIMULATION: If ID contains "0", we treat it as "Not Found" in the database.
        // This allows testing the 404 handler.
        if (id.contains("0")) {
            log.warn("[BackendService] getUserById() — user id={} not found in database", id);
            throw new ResourceNotFoundException("User with ID '" + id + "' was not found in our records.");
        }

        // For demo: construct a deterministic user profile from the ID
        UserResponse user = new UserResponse(
                id,
                "User_" + id,                                    // Name derived from ID
                "user" + id + "@example.com",                   // Email derived from ID
                Instant.now().minusSeconds(86400L * 30)        // Static age for demo
        );

        log.info("[BackendService] getUserById() — user={} resolved successfully", id);

        return new BackendResponse("User fetched successfully", user);
    }

    /**
     * Simulates a computationally expensive or I/O-heavy background operation.
     *
     * Real-world equivalent: triggering an async job (image resize, report generation,
     * email dispatch), calling an external payment API, or persisting a complex
     * transaction across multiple tables.
     *
     * We deliberately add latency here to demonstrate that even slow endpoints
     * are protected by the rate limiter — the interceptor runs before this method,
     * so rate limiting doesn't wait for the business logic to finish.
     *
     * @return BackendResponse confirming the processing result
     */
    public BackendResponse processRequest() {
        log.info("[BackendService] processRequest() called — beginning simulated processing");

        // Simulate real processing time (database write, external API call, etc.)
        simulateLatency();

        Map<String, Object> result = Map.of(
                "status",          "completed",
                "jobId",           "job-" + System.nanoTime(),     // Unique job ID per invocation
                "processingTimeMs", SIMULATED_LATENCY_MS,
                "message",         "Request processed and queued for downstream delivery.",
                "note",            "In production, this would enqueue to Kafka or trigger an async job."
        );

        log.info("[BackendService] processRequest() — processing complete, result dispatched");

        return new BackendResponse("Processing complete", result);
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Simulates network I/O or database latency.
     *
     * Why Thread.sleep() here and not in the controller?
     * Because latency is a property of the SERVICE (data access, computation),
     * not the HTTP layer. Controllers should be as thin as possible.
     *
     * Why catch InterruptedException?
     * Thread.sleep() can be interrupted (e.g., on server shutdown or test teardown).
     * Re-interrupting the thread (Thread.currentThread().interrupt()) is the correct
     * Java concurrency contract — it signals to any calling framework that the thread
     * was interrupted so it can clean up properly.
     */
    private void simulateLatency() {
        try {
            Thread.sleep(SIMULATED_LATENCY_MS);
        } catch (InterruptedException e) {
            // Restore the interrupted status — don't swallow the signal
            Thread.currentThread().interrupt();
            log.warn("[BackendService] simulateLatency() interrupted — processing may be incomplete");
        }
    }
}
