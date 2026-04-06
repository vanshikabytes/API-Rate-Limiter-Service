package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * EMPLOYEE REPOSITORY — In-Memory Data Access Layer
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * What is this class?
 * ─────────────────────────────────────────────────────────────────────────────
 * This is the Data Access Object (DAO) / Repository for Employee entities.
 * It replaces what would be a JpaRepository in a database-backed system.
 *
 * Instead of SQL, we use a ConcurrentHashMap<Long, Employee> as an in-memory
 * store. This approach is perfect for demos, tests, and interview projects
 * because it has zero infrastructure dependencies (no DB, no Docker).
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Why ConcurrentHashMap instead of HashMap?
 * ─────────────────────────────────────────────────────────────────────────────
 * Spring Boot handles requests on a thread pool — multiple HTTP requests
 * can arrive simultaneously. A plain HashMap is NOT thread-safe:
 *   ❌ Concurrent put() + get() can cause infinite loops (Java 7 bug)
 *   ❌ Visible memory inconsistencies between threads (JMM)
 *
 * ConcurrentHashMap is designed for concurrent access:
 *   ✔ Segment-level locking (fine-grained, not the full map)
 *   ✔ Atomic operations: putIfAbsent(), computeIfPresent(), etc.
 *   ✔ Thread-safe iteration (weakly consistent — safe for our read use-case)
 *   ✔ No need for synchronized blocks; the map handles its own locking
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Why AtomicLong for ID generation?
 * ─────────────────────────────────────────────────────────────────────────────
 * We need monotonically increasing IDs. Using a plain `long counter++` in a
 * multi-threaded environment creates race conditions (two threads could get
 * the same ID). AtomicLong.incrementAndGet() is a single, atomic CPU
 * instruction (CAS — Compare And Swap). It is:
 *   ✔ Lock-free (no synchronized keyword)
 *   ✔ Guaranteed unique across all threads
 *   ✔ The same pattern used by database AUTO_INCREMENT sequences
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * @Repository annotation:
 * ─────────────────────────────────────────────────────────────────────────────
 * Marks this class as a Spring-managed bean in the persistence layer.
 * Benefits:
 *   ✔ Enables component scanning (Spring finds it automatically)
 *   ✔ Enables Spring's exception translation (for JPA exceptions)
 *   ✔ Makes the layer explicit for readers and tooling
 */
@Repository
public class EmployeeRepository {

    private static final Logger log = LoggerFactory.getLogger(EmployeeRepository.class);

    /**
     * The in-memory "database" — maps Employee ID → Employee object.
     * ConcurrentHashMap ensures safe concurrent reads and writes
     * across multiple request-handling threads.
     */
    private final ConcurrentHashMap<Long, Employee> database = new ConcurrentHashMap<>();

    /**
     * Thread-safe ID counter. Starts at 3 because we pre-seed 3 employees.
     * Each new save() call increments this and uses the result as the new ID.
     */
    private final AtomicLong idCounter = new AtomicLong(3);

    // ─────────────────────────────────────────────────────────────────────────
    // Seed Data — Pre-populated employees for immediate testing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initializer block — runs once when the Spring bean is constructed.
     *
     * We pre-load 3 employees so that GET /api/backend/employees returns
     * meaningful data immediately without needing to POST first.
     *
     * In a JPA-based project, this would be done via:
     *   - @PostConstruct method
     *   - data.sql / import.sql
     *   - Spring Boot's ApplicationRunner / CommandLineRunner
     */
    {
        log.info("[EmployeeRepository] Seeding in-memory database with 3 sample employees...");

        database.put(1L, new Employee(1L, "Alice Johnson",  "alice.johnson@company.com",  "Engineering"));
        database.put(2L, new Employee(2L, "Bob Martinez",   "bob.martinez@company.com",   "Product"));
        database.put(3L, new Employee(3L, "Carol Williams", "carol.williams@company.com", "Human Resources"));

        log.info("[EmployeeRepository] Seed complete — {} employees loaded.", database.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD Operations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persist a new Employee to the in-memory store.
     *
     * Generates a unique ID via AtomicLong.incrementAndGet(), assigns it
     * to the employee, then stores the employee in the map.
     *
     * Equivalent to: INSERT INTO employees (...) VALUES (...) RETURNING id;
     *
     * @param employee the employee to save (id field will be set here)
     * @return the saved Employee with its assigned ID
     */
    public Employee save(Employee employee) {
        long newId = idCounter.incrementAndGet();
        employee.setId(newId);
        database.put(newId, employee);
        log.info("[EmployeeRepository] Saved new employee: id={}, name={}", newId, employee.getName());
        return employee;
    }

    /**
     * Retrieve a single Employee by its ID.
     *
     * Returns Optional<Employee> (not nullable) to force callers to handle
     * the "not found" case explicitly. This is the modern Java best practice
     * to avoid NullPointerExceptions.
     *
     * Equivalent to: SELECT * FROM employees WHERE id = ?;
     *
     * @param id the employee ID to look up
     * @return Optional containing the Employee, or Optional.empty() if not found
     */
    public Optional<Employee> findById(Long id) {
        return Optional.ofNullable(database.get(id));
    }

    /**
     * Retrieve all employees in the in-memory store.
     *
     * Returns a new ArrayList copy so callers cannot directly modify the
     * internal map's value collection. Defensive copy pattern.
     *
     * Equivalent to: SELECT * FROM employees;
     *
     * @return immutable snapshot of all employees
     */
    public List<Employee> findAll() {
        return new ArrayList<>(database.values());
    }

    /**
     * Update an existing employee's fields.
     *
     * We fetch the existing record, apply the new field values, then
     * put it back. ConcurrentHashMap.put() is atomic for the key assignment.
     *
     * Why not replace the entire object?
     * If an employee has fields we don't expose (e.g., createdAt), we
     * would lose them. Instead, we apply only the fields from the update payload.
     *
     * Equivalent to: UPDATE employees SET name=?, email=?, department=? WHERE id=?;
     *
     * @param id      the ID of the employee to update
     * @param updated the new field values
     * @return Optional of the updated Employee, or empty if not found
     */
    public Optional<Employee> update(Long id, Employee updated) {
        return Optional.ofNullable(database.computeIfPresent(id, (key, existing) -> {
            existing.setName(updated.getName());
            existing.setEmail(updated.getEmail());
            existing.setDepartment(updated.getDepartment());
            log.info("[EmployeeRepository] Updated employee: id={}", id);
            return existing;
        }));
    }

    /**
     * Remove an employee from the in-memory store.
     *
     * Returns true if the employee existed and was removed,
     * false if no employee with that ID existed.
     *
     * Equivalent to: DELETE FROM employees WHERE id = ?;
     *
     * @param id the ID of the employee to delete
     * @return true if deleted, false if not found
     */
    public boolean delete(Long id) {
        boolean existed = database.remove(id) != null;
        if (existed) {
            log.info("[EmployeeRepository] Deleted employee: id={}", id);
        } else {
            log.warn("[EmployeeRepository] Delete attempted on non-existent employee: id={}", id);
        }
        return existed;
    }
}
