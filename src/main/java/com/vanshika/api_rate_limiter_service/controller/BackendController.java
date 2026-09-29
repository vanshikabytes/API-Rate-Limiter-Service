package com.vanshika.api_rate_limiter_service.controller;

import lombok.extern.slf4j.Slf4j;

import com.vanshika.api_rate_limiter_service.dto.BackendResponse;
import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import com.vanshika.api_rate_limiter_service.service.BackendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for protected backend resources.
 * 
 * Notice there is NO rate-limiting code here. All enforcement is handled by the 
 * RateLimitInterceptor, which intercepts requests before they reach these methods.
 * This keeps our business logic separate from traffic management.
 */
@RestController
@RequestMapping("/api/backend")
@Slf4j
public class BackendController {
    private final BackendService backendService;

    // We use constructor injection for better testability and immutability.
    public BackendController(BackendService backendService) {
        this.backendService = backendService;
    }

    /**
     * Simulation of a data retrieval endpoint.
     */
    @GetMapping("/data")
    public ResponseEntity<ApiResponse<BackendResponse>> getData() {
        log.info("[BackendController] GET /api/backend/data — invoking BackendService.getData()");

        BackendResponse result = backendService.getData();

        log.info("[BackendController] GET /api/backend/data — requestId={}", result.getRequestId());

        return ResponseEntity.ok(
                new ApiResponse<>(true, result.getMessage(), result)
        );
    }

    /**
     * Simulation of a user lookup.
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<BackendResponse>> getUserById(@PathVariable String id) {
        log.info("[BackendController] GET /api/backend/users/{} — invoking BackendService.getUserById()", id);

        BackendResponse result = backendService.getUserById(id);

        log.info("[BackendController] GET /api/backend/users/{} — requestId={}", id, result.getRequestId());

        return ResponseEntity.ok(
                new ApiResponse<>(true, result.getMessage(), result)
        );
    }

    /**
     * Simulation of a state-changing operation (POST).
     */
    @PostMapping("/process")
    public ResponseEntity<ApiResponse<BackendResponse>> processRequest() {
        log.info("[BackendController] POST /api/backend/process — invoking BackendService.processRequest()");

        BackendResponse result = backendService.processRequest();

        log.info("[BackendController] POST /api/backend/process — requestId={}", result.getRequestId());

        return ResponseEntity.ok(
                new ApiResponse<>(true, result.getMessage(), result)
        );
    }
}

