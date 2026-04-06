package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.exception.ResourceNotFoundException;
import com.vanshika.api_rate_limiter_service.model.Employee;
import com.vanshika.api_rate_limiter_service.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * EMPLOYEE SERVICE — Business Logic Layer
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * What is this class?
 * ─────────────────────────────────────────────────────────────────────────────
 * The service layer sits between the Controller (HTTP) and the Repository
 * (Data). It is responsible for:
 *   ✔ Business rule validation (e.g., duplicate email check — extensible)
 *   ✔ Orchestrating calls to one or more repositories
 *   ✔ Translating data-layer "Optional.empty()" into meaningful domain
 *     exceptions (ResourceNotFoundException → HTTP 404 via GlobalExceptionHandler)
 *   ✔ Logging significant business events
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Separation of Concerns (why 3 layers?):
 * ─────────────────────────────────────────────────────────────────────────────
 *
 *   ┌──────────────────────────────────────┐
 *   │  EmployeeController  (HTTP layer)    │  Maps HTTP ↔ service calls
 *   ├──────────────────────────────────────┤
 *   │  EmployeeService     (Business layer)│  ← YOU ARE HERE
 *   ├──────────────────────────────────────┤
 *   │  EmployeeRepository  (Data layer)    │  Reads/writes from ConcurrentHashMap
 *   └──────────────────────────────────────┘
 *
 * Each layer only knows about the one below it. The controller does NOT
 * talk to the repository directly — this ensures business rules can be
 * changed in one place without affecting the HTTP layer.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Why throw ResourceNotFoundException here and not in the repository?
 * ─────────────────────────────────────────────────────────────────────────────
 * The repository's job is data retrieval — returning Optional<Employee>
 * is neutral. The business decision to call a missing record an "error"
 * belongs to the service layer, which understands domain semantics.
 *
 * GlobalExceptionHandler catches ResourceNotFoundException and maps it to
 * HTTP 404 — so the controller never has to check null or Optional.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Rate Limiting Note:
 * ─────────────────────────────────────────────────────────────────────────────
 * There is ZERO rate-limiting code in this service.
 * By the time any method here runs, the RateLimitInterceptor has already:
 *   1. Identified the caller (X-User-Id header or IP)
 *   2. Consumed a token from their bucket
 *   3. Set X-RateLimit-* headers on the response
 *
 * This service simply executes business logic — it doesn't know or care
 * whether rate limiting is in place. This is the middleware pattern.
 */
@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    /**
     * Constructor injection — the preferred Spring pattern.
     * Makes dependencies explicit, immutable, and easy to test with mocks.
     */
    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates and persists a new Employee.
     *
     * The employee's id field should be null on entry — the repository
     * assigns a unique ID via AtomicLong before persisting.
     *
     * Business extension point: Add duplicate-email check here before saving.
     *
     * @param employee the employee data from the POST request body
     * @return the saved Employee with its assigned ID
     */
    public Employee createEmployee(Employee employee) {
        log.info("[EmployeeService] Creating new employee: name={}, department={}",
                employee.getName(), employee.getDepartment());

        Employee saved = employeeRepository.save(employee);

        log.info("[EmployeeService] Employee created successfully: id={}", saved.getId());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ (single)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches a single Employee by their unique ID.
     *
     * If the ID does not exist in the repository, throws
     * ResourceNotFoundException, which GlobalExceptionHandler maps to HTTP 404.
     *
     * @param id the employee ID from the URL path variable
     * @return the found Employee
     * @throws ResourceNotFoundException if no employee with the given ID exists
     */
    public Employee getEmployee(Long id) {
        log.info("[EmployeeService] Looking up employee: id={}", id);

        return employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[EmployeeService] Employee not found: id={}", id);
                    return new ResourceNotFoundException("Employee not found with ID: " + id);
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ (all)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all employees in the system.
     *
     * Returns an empty list (not null, not an exception) if no employees exist.
     * Callers should always expect a list — even if it is empty.
     *
     * @return list of all employees (may be empty)
     */
    public List<Employee> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        log.info("[EmployeeService] Fetched all employees — count={}", employees.size());
        return employees;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates the fields of an existing Employee.
     *
     * The ID is derived from the URL path, not from the request body,
     * to prevent clients from accidentally changing their own ID.
     *
     * @param id      the ID of the employee to update
     * @param updated the new field values from the PUT request body
     * @return the updated Employee
     * @throws ResourceNotFoundException if no employee with the given ID exists
     */
    public Employee updateEmployee(Long id, Employee updated) {
        log.info("[EmployeeService] Updating employee: id={}", id);

        return employeeRepository.update(id, updated)
                .orElseThrow(() -> {
                    log.warn("[EmployeeService] Cannot update — employee not found: id={}", id);
                    return new ResourceNotFoundException("Cannot update. Employee not found with ID: " + id);
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deletes an Employee by ID.
     *
     * In a real application this might be a "soft delete" (setting an
     * is_deleted flag) rather than a physical removal. For simplicity, we
     * perform a hard delete from the map.
     *
     * @param id the ID of the employee to delete
     * @throws ResourceNotFoundException if no employee with the given ID exists
     */
    public void deleteEmployee(Long id) {
        log.info("[EmployeeService] Deleting employee: id={}", id);

        boolean deleted = employeeRepository.delete(id);
        if (!deleted) {
            log.warn("[EmployeeService] Cannot delete — employee not found: id={}", id);
            throw new ResourceNotFoundException("Cannot delete. Employee not found with ID: " + id);
        }

        log.info("[EmployeeService] Employee deleted successfully: id={}", id);
    }
}
