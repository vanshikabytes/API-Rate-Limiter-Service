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
 * Service for core backend business logic.
 * 
 * Separating business logic from the rate-limiting infrastructure ensures 
 * that we follow the Single Responsibility Principle. This service only cares 
 * about processing data, not who is calling it or how often.
 */
@Service
public class BackendService {

    private static final Logger log = LoggerFactory.getLogger(BackendService.class);

    // Simulate real-world database or network latency.
    private static final long SIMULATED_LATENCY_MS = 50;

    /**
     * Fetches generic summary data.
     */
    public BackendResponse getData() {
        log.info("[BackendService] getData() called");

        Map<String, Object> payload = Map.of(
                "source",      "in-memory-store",
                "recordCount", 42,
                "status",      "healthy",
                "environment", "demo"
        );

        return new BackendResponse("Data retrieved successfully", payload);
    }

    /**
     * Fetches details for a specific user ID.
     */
    public BackendResponse getUserById(String id) {
        log.info("[BackendService] getUserById() called for id={}", id);

        simulateLatency();

        // For demonstration: any ID containing '0' is treated as a missing record.
        if (id.contains("0")) {
            log.warn("[BackendService] User not found: id={}", id);
            throw new ResourceNotFoundException("User with ID '" + id + "' was not found.");
        }

        UserResponse user = new UserResponse(
                id,
                "User_" + id,
                "user" + id + "@example.com",
                Instant.now().minusSeconds(86400L * 30)
        );

        return new BackendResponse("User fetched successfully", user);
    }

    /**
     * Simulates a heavy processing task.
     */
    public BackendResponse processRequest() {
        log.info("[BackendService] processRequest() called");

        simulateLatency();

        Map<String, Object> result = Map.of(
                "status",          "completed",
                "jobId",           "job-" + System.nanoTime(),
                "processingTimeMs", SIMULATED_LATENCY_MS
        );

        return new BackendResponse("Processing complete", result);
    }

    /**
     * Utility to pause execution, mimicking real backend work.
     */
    private void simulateLatency() {
        try {
            Thread.sleep(SIMULATED_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            log.warn("[BackendService] Latency simulation interrupted");
        }
    }
}
