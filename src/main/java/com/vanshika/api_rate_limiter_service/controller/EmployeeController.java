package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import com.vanshika.api_rate_limiter_service.model.Employee;
import com.vanshika.api_rate_limiter_service.service.EmployeeService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EMPLOYEE CONTROLLER — HTTP Layer for the Employee Management System
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * What is this class?
 * ─────────────────────────────────────────────────────────────────────────────
 * This is a thin HTTP adapter. Its ONLY responsibilities are:
 *   1. Map incoming HTTP requests to the correct EmployeeService method.
 *   2. Return a ResponseEntity<ApiResponse<T>> with the correct HTTP status.
 *   3. Log the entry and exit of each request.
 *
 * ALL business logic lives in EmployeeService.
 * ALL validation is triggered by @Valid on the @RequestBody parameter.
 * ALL rate limiting is enforced by RateLimitInterceptor (WebConfig.java).
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Why does this controller have NO rate-limiting code?
 * ─────────────────────────────────────────────────────────────────────────────
 * Rate limiting is a cross-cutting concern registered in WebConfig:
 *
 *   registry.addInterceptor(rateLimitInterceptor)
 *           .addPathPatterns("/api/backend/**");
 *
 * Since this controller's base path is /api/backend/employees, which matches
 * /api/backend/**, ALL endpoints here are automatically protected.
 *
 * Request flow:
 *   HTTP Request → RateLimitInterceptor.preHandle() → EmployeeController → EmployeeService
 *                  ↑ Enforces token bucket, sets headers, throws 429 if needed
 *
 * The controller never executes unless a token was successfully consumed.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Why ApiResponse<T> envelope?
 * ─────────────────────────────────────────────────────────────────────────────
 * Every endpoint returns the same top-level shape:
 *   {
 *     "success": true | false,
 *     "message": "Human-readable description",
 *     "data": { ... }     ← typed payload
 *   }
 *
 * This allows clients to:
 *   1. Always check "success" first (no HTTP status interpretation needed)
 *   2. Extract "data" safely when success is true
 *   3. Read "message" for user-facing error strings when success is false
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Real-World Relevance (Interview Talking Point):
 * ─────────────────────────────────────────────────────────────────────────────
 * This architecture (thin controller + service + repository + middleware) is
 * used by every production Spring Boot system at scale:
 *   - Spring Security uses FilterChain (not controllers) for auth
 *   - Netflix Hystrix / Resilience4j wraps service calls (not controllers)
 *   - AWS API Gateway enforces throttle BEFORE routing to Lambda handlers
 *
 * The controller has ONE responsibility. That makes it easy to test, change,
 * and reason about independently of the middleware.
 */
@RestController
@RequestMapping("/api/backend/employees")
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    /**
     * Constructor injection — not field injection (@Autowired).
     * Reasons: explicit dependencies, immutability (final), testability.
     */
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/backend/employees  →  Create a new employee
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new Employee from the JSON request body.
     *
     * @Valid triggers Bean Validation on the Employee fields:
     *   - name:       @NotBlank, @Size(min=2, max=100)
     *   - email:      @NotBlank, @Email
     *   - department: @NotBlank, @Size(max=100)
     *
     * If validation fails, Spring throws MethodArgumentNotValidException
     * which GlobalExceptionHandler converts to a 400 Bad Request response.
     *
     * HTTP 201 CREATED is returned (not 200 OK) because a new resource was
     * created. This is semantically correct per RFC 7231.
     *
     * @param employee the employee payload from the request body
     * @return 201 Created with ApiResponse<Employee>
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Employee>> createEmployee(
            @Valid @RequestBody Employee employee) {

        log.info("[EmployeeController] POST /api/backend/employees — creating employee: name={}",
                employee.getName());

        Employee created = employeeService.createEmployee(employee);

        log.info("[EmployeeController] Employee created: id={}", created.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Employee created successfully.", created));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/backend/employees/{id}  →  Get a single employee by ID
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a single Employee identified by the URL path variable {id}.
     *
     * If the employee does not exist, EmployeeService throws
     * ResourceNotFoundException → GlobalExceptionHandler returns HTTP 404.
     *
     * @param id the employee ID from the URL (e.g., /api/backend/employees/1)
     * @return 200 OK with ApiResponse<Employee>
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Employee>> getEmployee(@PathVariable Long id) {
        log.info("[EmployeeController] GET /api/backend/employees/{}", id);

        Employee employee = employeeService.getEmployee(id);

        log.info("[EmployeeController] Returning employee: id={}, name={}", id, employee.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Employee retrieved successfully.", employee)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/backend/employees  →  Get all employees
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a list of all employees currently in the system.
     *
     * This list is pre-populated with 3 seed employees on startup
     * (see EmployeeRepository initializer block).
     *
     * Returns an empty array [] if no employees exist — never null.
     *
     * @return 200 OK with ApiResponse<List<Employee>>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Employee>>> getAllEmployees() {
        log.info("[EmployeeController] GET /api/backend/employees — listing all employees");

        List<Employee> employees = employeeService.getAllEmployees();

        log.info("[EmployeeController] Returning {} employees", employees.size());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Employees retrieved successfully. Total: " + employees.size(), employees)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/backend/employees/{id}  →  Update an existing employee
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates the fields of an existing Employee.
     *
     * The ID comes from the URL path — the request body should NOT include an
     * id field (if it does, it is ignored by the service for safety).
     *
     * @Valid enforces the same constraints as POST (name, email, department).
     *
     * Returns HTTP 200 OK (not 204) so we can include the updated object
     * in the response body — helpful for clients to confirm the changes.
     *
     * @param id      the employee ID from the URL path
     * @param updated the new field values from the request body
     * @return 200 OK with ApiResponse<Employee> containing the updated object
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Employee>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody Employee updated) {

        log.info("[EmployeeController] PUT /api/backend/employees/{} — updating employee", id);

        Employee employee = employeeService.updateEmployee(id, updated);

        log.info("[EmployeeController] Employee updated: id={}", id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Employee updated successfully.", employee)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/backend/employees/{id}  →  Delete an employee
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deletes an Employee by ID.
     *
     * Returns HTTP 200 OK (with a confirmation message) rather than the more
     * common 204 No Content. This is a deliberate choice so the ApiResponse
     * envelope is always present — making client parsing consistent.
     *
     * If the employee does not exist, EmployeeService throws
     * ResourceNotFoundException → GlobalExceptionHandler returns HTTP 404.
     *
     * @param id the employee ID from the URL path
     * @return 200 OK with ApiResponse<String> confirming deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteEmployee(@PathVariable Long id) {
        log.info("[EmployeeController] DELETE /api/backend/employees/{}", id);

        employeeService.deleteEmployee(id);

        log.info("[EmployeeController] Employee deleted: id={}", id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Employee deleted successfully.", "Deleted employee with ID: " + id)
        );
    }
}
