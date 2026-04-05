package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.dto.BackendResponse;
import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import com.vanshika.api_rate_limiter_service.service.BackendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * BACKEND CONTROLLER — Step 5: Pure Business Logic, Zero Rate-Limit Code
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Why is there NO rate-limiting code in this controller?
 * ─────────────────────────────────────────────────────────────────────────────
 * This is intentional and is the core architectural goal of Step 4–5.
 *
 * The rate limiter is a cross-cutting concern — it applies to ALL endpoints
 * uniformly. Putting rate-limit checks inside individual controller methods
 * would mean:
 *   ❌ Duplicated logic across every endpoint
 *   ❌ Easy to forget on a new endpoint → accidental bypass
 *   ❌ Impossible to change the rate-limit strategy in one place
 *   ❌ Controller mixes two responsibilities (HTTP + enforcement)
 *
 * Instead, RateLimitInterceptor is registered via WebConfig to intercept
 * ALL /api/backend/** paths BEFORE this controller is ever reached.
 *
 * The flow is:
 *
 *   HTTP Request
 *       ↓
 *   RateLimitInterceptor.preHandle()   ← enforces token-bucket limit
 *       ↓ if allowed (token consumed)
 *   BackendController method           ← pure business logic, no rate code
 *       ↓
 *   HTTP Response
 *
 * Any request that reaches a method below has ALREADY passed rate limiting.
 * This controller cannot be bypassed — the interceptor always runs first.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Why use @RestController + ResponseEntity<ApiResponse<BackendResponse>>?
 * ─────────────────────────────────────────────────────────────────────────────
 * @RestController = @Controller + @ResponseBody → Jackson auto-serializes to JSON.
 *
 * ResponseEntity gives us full control of the HTTP response:
 *   - Status code (200, 201, 404, etc.)
 *   - Headers (e.g., Location, ETag)
 *   - Body (typed to ApiResponse<BackendResponse>)
 *
 * ApiResponse<BackendResponse> enforces the standard envelope:
 *   {
 *     "success": true,
 *     "message": "...",
 *     "data": { "requestId": "...", "timestamp": "...", ... }
 *   }
 *
 * This means EVERY endpoint in the backend returns the same top-level shape.
 * Clients can always check `success` first before parsing `data`.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Real-world relevance:
 * ─────────────────────────────────────────────────────────────────────────────
 * This pattern (thin controller → rich service → typed DTO) is used by:
 *   - Netflix OSS (Hystrix / Resilience4j wraps service calls, not controllers)
 *   - Spring Security (SecurityFilterChain intercepts before controllers)
 *   - AWS API Gateway (middleware enforces auth/throttle before Lambda handler)
 *
 * The controller has ONE responsibility: map HTTP ↔ service calls.
 */
@RestController
@RequestMapping("/api/backend")
public class BackendController {

    private static final Logger log = LoggerFactory.getLogger(BackendController.class);

    /**
     * BackendService encapsulates all business logic.
     * Controller delegates immediately — no logic lives here.
     *
     * Constructor injection (not @Autowired field injection) is preferred because:
     *   - Makes dependencies explicit and visible
     *   - Supports immutability (field can be final)
     *   - Enables easier unit testing (inject mocks directly)
     */
    private final BackendService backendService;

    public BackendController(BackendService backendService) {
        this.backendService = backendService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Endpoint 1: GET /api/backend/data
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a simulated generic data payload.
     *
     * Protected by: RateLimitInterceptor
     * By the time execution reaches this line, the interceptor has:
     *   1. Extracted the client key (X-User-Id header or IP fallback)
     *   2. Consumed 1 token from the client's bucket
     *   3. Set X-RateLimit-Remaining / X-RateLimit-Capacity headers on the response
     *
     * Real-world: This endpoint would aggregate data from multiple sources —
     * a SQL database, a Redis cache, and maybe an external REST API —
     * then merge and return a unified dashboard summary.
     *
     * @return 200 OK with ApiResponse wrapping a BackendResponse DTO
     */
    @GetMapping("/data")
    public ResponseEntity<ApiResponse<BackendResponse>> getData() {
        log.info("[BackendController] GET /api/backend/data — invoking BackendService.getData()");

        // Delegate entirely to the service layer — no logic in the controller
        BackendResponse result = backendService.getData();

        log.info("[BackendController] GET /api/backend/data — requestId={}", result.getRequestId());

        // Wrap in standard ApiResponse envelope for consistent response shape
        return ResponseEntity.ok(
                new ApiResponse<>(true, result.getMessage(), result)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Endpoint 2: GET /api/backend/users/{id}
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches a simulated user profile for the given ID.
     *
     * @PathVariable binds the {id} segment from the URL to the method parameter.
     * Real-world: This would call UserRepository.findById(id) and map the
     * JPA entity to a UserResponse DTO before returning.
     *
     * Why return a typed UserResponse (inside BackendResponse) instead of a Map?
     * See UserResponse.java for the full explanation — in short:
     *   - Compile-time safety
     *   - Consistent serialization
     *   - OpenAPI documentation support
     *
     * @param id User identifier from URL path (e.g., /api/backend/users/42)
     * @return 200 OK with ApiResponse wrapping a BackendResponse containing a UserResponse
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

    // ─────────────────────────────────────────────────────────────────────────
    // Endpoint 3: POST /api/backend/process
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Simulates a computationally intensive or I/O-heavy processing operation.
     *
     * Why POST and not GET?
     * HTTP semantics: GET is for fetching data (idempotent, cacheable).
     *                 POST is for triggering operations that change state.
     * Processing a request (e.g., charging a card, sending an email, resizing an image)
     * is a state-changing operation → POST is semantically correct.
     *
     * This endpoint deliberately adds ~50ms latency inside the service to simulate
     * real backend processing time. This demonstrates that:
     *   1. The rate limiter intercepts BEFORE the slow work begins
     *   2. Rate limiting adds essentially zero overhead to the request pipeline
     *   3. Even slow endpoints cannot be abused — they're protected equally
     *
     * Real-world: This would publish a message to a Kafka topic, trigger a
     * background job, or call an external payment gateway.
     *
     * @return 200 OK with ApiResponse wrapping a BackendResponse confirming processing
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
