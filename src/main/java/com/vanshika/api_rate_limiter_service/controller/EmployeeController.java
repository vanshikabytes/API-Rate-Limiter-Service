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
 * REST controller for Employee CRUD operations.
 * 
 * This controller is automatically protected by our RateLimitInterceptor
 * because
 * its path falls under '/api/backend/**'.
 * 
 * We use a standard ApiResponse envelope for all responses to ensure a
 * consistent
 * experience for the frontend.
 */
@RestController
@RequestMapping("/api/backend/employees")
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Creates a new employee.
     * We use @Valid to enforce field constraints (name, email, etc.) at the entry
     * point.
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

    /**
     * Fetches a single employee by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Employee>> getEmployee(
            @PathVariable Long id,
            @RequestParam(required = false) String name) {
        log.info("[EmployeeController] GET /api/backend/employees/{} with filter name={}", id, name);

        Employee employee = employeeService.getEmployee(id);

        if (name != null && !name.equalsIgnoreCase(employee.getName())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, "No employee found matching both ID=" + id + " and name=" + name, null));
        }

        log.info("[EmployeeController] Returning employee: id={}, name={}", id, employee.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Employee retrieved successfully.", employee));
    }

    /**
     * Lists all employees.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Employee>>> getAllEmployees() {
        log.info("[EmployeeController] GET /api/backend/employees — listing all employees");

        List<Employee> employees = employeeService.getAllEmployees();

        log.info("[EmployeeController] Returning {} employees", employees.size());

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Employees retrieved successfully. Total: " + employees.size(), employees));
    }

    /**
     * Updates an existing employee.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Employee>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody Employee updated) {

        log.info("[EmployeeController] PUT /api/backend/employees/{} — updating employee", id);

        Employee employee = employeeService.updateEmployee(id, updated);

        log.info("[EmployeeController] Employee updated: id={}", id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Employee updated successfully.", employee));
    }

    /**
     * Deletes an employee by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteEmployee(@PathVariable Long id) {
        log.info("[EmployeeController] DELETE /api/backend/employees/{}", id);

        employeeService.deleteEmployee(id);

        log.info("[EmployeeController] Employee deleted: id={}", id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Employee deleted successfully.", "Deleted employee with ID: " + id));
    }
}
