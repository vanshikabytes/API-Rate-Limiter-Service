package com.vanshika.api_rate_limiter_service.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Domain model representing an Employee.
 * 
 * We use Jakarta Bean Validation constraints to ensure data integrity at the 
 * entry point. This avoids polluting our service layer with basic null or 
 * format checks.
 */
public class Employee {

    // Unique ID assigned by the repository after persistence.
    private Long id;

    @NotBlank(message = "Employee name must not be blank")
    @Size(min = 2, max = 100, message = "Employee name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Employee email must not be blank")
    @Email(message = "Employee email must be a valid email address")
    private String email;

    @NotBlank(message = "Employee department must not be blank")
    @Size(max = 100, message = "Department name must not exceed 100 characters")
    private String department;

    // Jackson requires a no-arg constructor for deserialization.
    public Employee() {}

    public Employee(Long id, String name, String email, String department) {
        this.id         = id;
        this.name       = name;
        this.email      = email;
        this.department = department;
    }

    // Standard getters and setters for visibility and Jackson serialization.
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
