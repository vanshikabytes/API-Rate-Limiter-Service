package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.dto.CreateUserRequest;
import com.vanshika.api_rate_limiter_service.dto.UpdateTierRequest;
import com.vanshika.api_rate_limiter_service.exception.ResourceNotFoundException;
import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import com.vanshika.api_rate_limiter_service.model.User;
import com.vanshika.api_rate_limiter_service.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * REST controller for User Management operations.
 * 
 * This controller provides administrative endpoints to manage users and their
 * tiers.
 * Note that this controller is NOT under /api/backend/**, so it is not subject
 * to the rate limiting interceptor, avoiding circular rate limiting logic.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * In-memory store for user data.
     * Chosen to keep identity management lightweight for this phase.
     */
    private final UserRepository userRepository;

    /**
     * Constructor injection for the repository.
     */
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates a new user in the system.
     * 
     * @param request The data for the new user, including ID and name.
     * @return 201 Created with the saved user object.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = new User(request.getUserId(), request.getTier());
        User savedUser = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "User created successfully", savedUser));
    }

    /**
     * Lists all registered users.
     * 
     * @return 200 OK with a list of all users in the system.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Collection<User>>> getAllUsers() {
        Collection<User> users = userRepository.findAll();
        return ResponseEntity.ok(new ApiResponse<>(true, "Users retrieved successfully", users));
    }

    /**
     * Retrieves a specific user by their ID.
     * 
     * @param userId The unique identifier to look up.
     * @return 200 OK or 404 via ResourceNotFoundException.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return ResponseEntity.ok(new ApiResponse<>(true, "User found", user));
    }

    /**
     * Assigns or updates the tier of an existing user.
     * This update directly impacts how RateLimiterService resolves limits for this
     * user.
     * 
     * @param userId  The ID of the user to upgrade/downgrade.
     * @param request The new tier level (free, gold, platinum).
     * @return 200 OK with the updated user details.
     */
    @PatchMapping("/{userId}/tier")
    public ResponseEntity<ApiResponse<User>> updateTier(
            @PathVariable String userId,
            @Valid @RequestBody UpdateTierRequest request) {

        User updatedUser = userRepository.updateTier(userId, request.getTier());
        return ResponseEntity.ok(new ApiResponse<>(true, "User tier updated to " + request.getTier(), updatedUser));
    }
}
