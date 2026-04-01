package com.vanshika.api_rate_limiter_service.service;

import org.springframework.stereotype.Service;

/**
 * Step 1: Backend Service
 * This service simulates a real backend business logic processing.
 * In a real-world scenario, the Rate Limiter acts as a middleware, 
 * and only if the request is allowed, this service is called.
 */
@Service
public class BackendService {

    /**
     * Simulates backend processing.
     * @return A success message from the backend.
     */
    public String processRequest() {
        // Business logic would go here
        return "Processed by backend - Data successfully retrieved!";
    }
}
