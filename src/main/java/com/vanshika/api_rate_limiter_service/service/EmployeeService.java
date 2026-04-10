package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.exception.ResourceNotFoundException;
import com.vanshika.api_rate_limiter_service.model.Employee;
import com.vanshika.api_rate_limiter_service.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles business logic for employee management.
 * 
 * This layer orchestrates data between the controller and repository. 
 * Notice that it doesn't contain any rate-limiting code — that's handled 
 * as a cross-cutting concern in the interceptor layer.
 */
@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);
    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Saves a new employee. IDs are generated automatically by the repository.
     */
    public Employee createEmployee(Employee employee) {
        log.info("[EmployeeService] Creating new employee: name={}, department={}",
                employee.getName(), employee.getDepartment());

        Employee saved = employeeRepository.save(employee);

        log.info("[EmployeeService] Employee created successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Finds an employee by ID. 
     * Throws ResourceNotFoundException if they don't exist, which maps to a 404 response.
     */
    public Employee getEmployee(Long id) {
        log.info("[EmployeeService] Looking up employee: id={}", id);

        return employeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[EmployeeService] Employee not found: id={}", id);
                    return new ResourceNotFoundException("Employee not found with ID: " + id);
                });
    }

    /**
     * Lists all employees in the system.
     */
    public List<Employee> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        log.info("[EmployeeService] Fetched all employees — count={}", employees.size());
        return employees;
    }

    /**
     * Updates an existing employee's details.
     */
    public Employee updateEmployee(Long id, Employee updated) {
        log.info("[EmployeeService] Updating employee: id={}", id);

        return employeeRepository.update(id, updated)
                .orElseThrow(() -> {
                    log.warn("[EmployeeService] Cannot update — employee not found: id={}", id);
                    return new ResourceNotFoundException("Cannot update. Employee not found with ID: " + id);
                });
    }

    /**
     * Deletes an employee from the system.
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
