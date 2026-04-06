package com.vanshika.api_rate_limiter_service.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * EMPLOYEE MODEL — Domain Entity for the Employee Management System
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * What is this class?
 * ─────────────────────────────────────────────────────────────────────────────
 * This is the core domain object representing an Employee in our system.
 * In a real production system this would be a JPA @Entity backed by a database
 * (e.g., PostgreSQL, MySQL). Here we use a plain POJO stored in an in-memory
 * ConcurrentHashMap to keep the focus on the rate-limiter architecture.
 *
 * This class replaces the old "dummy" BackendService that returned fake data
 * with no real structure. Now, every operation (create/read/update/delete)
 * works against a real domain object with validated fields.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Why explicit constructors + getters/setters instead of Lombok @Data?
 * ─────────────────────────────────────────────────────────────────────────────
 * While Lombok is available in the project, we write everything explicitly here
 * so the code is 100% readable during an interview or code review — no magic.
 * Each field, constructor, getter and setter is visible and intentional.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Why Bean Validation annotations (@NotBlank, @Email)?
 * ─────────────────────────────────────────────────────────────────────────────
 * We use Jakarta Bean Validation (spring-boot-starter-validation in pom.xml).
 * Annotations on model fields + @Valid in the controller parameter ensure that:
 *   - Bad data is rejected BEFORE reaching the service layer
 *   - Error messages are centralized in GlobalExceptionHandler
 *   - No null-check boilerplate inside service methods
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Separation of Concerns:
 * ─────────────────────────────────────────────────────────────────────────────
 *   Employee (model)        → pure data structure + constraints
 *   EmployeeRepository      → storage/retrieval
 *   EmployeeService         → business rules
 *   EmployeeController      → HTTP routing
 *   RateLimitInterceptor    → cross-cutting enforcement (unchanged)
 */
public class Employee {

    /**
     * Unique identifier for the employee.
     * Auto-assigned by the repository using an AtomicLong counter.
     * Null on creation (before being persisted); non-null thereafter.
     */
    private Long id;

    /**
     * Full name of the employee.
     * Must not be blank and must be between 2 and 100 characters.
     */
    @NotBlank(message = "Employee name must not be blank")
    @Size(min = 2, max = 100, message = "Employee name must be between 2 and 100 characters")
    private String name;

    /**
     * Corporate email address of the employee.
     * Must be a valid email format (e.g., alice@company.com).
     * Must not be blank.
     */
    @NotBlank(message = "Employee email must not be blank")
    @Email(message = "Employee email must be a valid email address")
    private String email;

    /**
     * Department the employee belongs to (e.g., Engineering, HR, Finance).
     * Must not be blank.
     */
    @NotBlank(message = "Employee department must not be blank")
    @Size(max = 100, message = "Department name must not exceed 100 characters")
    private String department;

    // ─────────────────────────────────────────────────────────────────────────
    // Constructors
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * No-arg constructor required for Jackson deserialization.
     * When Spring reads JSON from a POST/PUT request body, it calls this
     * constructor first and then uses the setters to populate each field.
     */
    public Employee() {}

    /**
     * Full constructor — used by the repository when loading sample seed data
     * or when returning a fully-populated object.
     *
     * @param id         the unique assigned ID
     * @param name       the employee's full name
     * @param email      the employee's email address
     * @param department the employee's department
     */
    public Employee(Long id, String name, String email, String department) {
        this.id         = id;
        this.name       = name;
        this.email      = email;
        this.department = department;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Getters — used by Jackson for serialization (object → JSON)
    // ─────────────────────────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setters — used by Jackson for deserialization (JSON → object)
    //           and by the repository for update operations
    // ─────────────────────────────────────────────────────────────────────────

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toString — useful for logging and debugging
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                '}';
    }
}
