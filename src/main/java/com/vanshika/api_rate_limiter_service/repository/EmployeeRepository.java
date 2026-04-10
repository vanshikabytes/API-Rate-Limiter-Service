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
 * In-memory data store for Employee entities.
 * 
 * We use ConcurrentHashMap and AtomicLong to ensure thread safety without the 
 * overhead of a full database. This simulates a real persistence layer for 
 * demonstration purposes.
 */
@Repository
public class EmployeeRepository {

    private static final Logger log = LoggerFactory.getLogger(EmployeeRepository.class);

    // Thread-safe map for storing employees by ID.
    private final ConcurrentHashMap<Long, Employee> database = new ConcurrentHashMap<>();

    // Atomic counter to generate unique IDs safely in a multi-threaded environment.
    private final AtomicLong idCounter = new AtomicLong(3);

    // Pre-populating the "database" with sample records for immediate testing.
    {
        log.info("[EmployeeRepository] Seeding in-memory database with initial records...");
        database.put(1L, new Employee(1L, "Alice Johnson",  "alice.johnson@company.com",  "Engineering"));
        database.put(2L, new Employee(2L, "Bob Martinez",   "bob.martinez@company.com",   "Product"));
        database.put(3L, new Employee(3L, "Carol Williams", "carol.williams@company.com", "Human Resources"));
    }

    /**
     * Saves a new employee and returns the persisted object with an assigned ID.
     */
    public Employee save(Employee employee) {
        long newId = idCounter.incrementAndGet();
        employee.setId(newId);
        database.put(newId, employee);
        log.info("[EmployeeRepository] Saved new employee: id={}, name={}", newId, employee.getName());
        return employee;
    }

    /**
     * Looks up an employee by ID. Returns an Optional to avoid null checks in the caller.
     */
    public Optional<Employee> findById(Long id) {
        return Optional.ofNullable(database.get(id));
    }

    /**
     * Returns a snapshot of all employees.
     */
    public List<Employee> findAll() {
        return new ArrayList<>(database.values());
    }

    /**
     * Updates an existing employee's details using computeIfPresent for thread-safe mapping.
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
     * Removes an employee from the store.
     */
    public boolean delete(Long id) {
        boolean existed = database.remove(id) != null;
        if (existed) {
            log.info("[EmployeeRepository] Deleted employee: id={}", id);
        }
        return existed;
    }
}
