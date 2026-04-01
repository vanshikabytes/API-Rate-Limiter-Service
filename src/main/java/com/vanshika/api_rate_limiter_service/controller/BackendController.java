package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.service.BackendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Step 1: Backend Controller
 * This represents the "Backend Service" mentioned in the architecture:
 * Client -> Rate Limiter (Middleware) -> Backend Service -> Response
 * 
 * Note: In a true middleware setup, this controller would be called 
 * transparently, but for simulation, we can call the service from 
 * the RateLimiterController.
 */
@RestController
@RequestMapping("/api/backend")
public class BackendController {

    private final BackendService backendService;

    public BackendController(BackendService backendService) {
        this.backendService = backendService;
    }

    /**
     * A sample backend endpoint.
     * @return Simulated data from the backend.
     */
    @GetMapping("/data")
    public String getData() {
        return backendService.processRequest();
    }
}
