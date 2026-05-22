package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.exception.ResourceNotFoundException;
import com.vanshika.api_rate_limiter_service.model.User;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory repository for User entities.
 *
 * Uses ConcurrentHashMap for high-concurrency access, similar to how
 * token buckets are stored. This avoids the need for external database
 * dependencies for this phase.
 */
@Repository
public class UserRepository {

    /**
     * Internal store for users, keyed by their unique userId.
     * ConcurrentHashMap ensures thread-safety during parallel user updates or lookups.
     */
    private final ConcurrentHashMap<String, User> userStore = new ConcurrentHashMap<>();

    /**
     * Saves a new user to the repository.
     *
     * @param user The user object to persist.
     * @return The saved user.
     * @throws IllegalArgumentException if the userId already exists.
     */
    public User save(User user) {
        if (userStore.containsKey(user.getUserId())) {
            throw new IllegalArgumentException("User already exists with ID: " + user.getUserId());
        }
        userStore.put(user.getUserId(), user);
        return user;
    }

    /**
     * Finds a user by their unique identifier.
     *
     * @param userId The ID to search for.
     * @return An Optional containing the User if found, empty otherwise.
     */
    public Optional<User> findById(String userId) {
        return Optional.ofNullable(userStore.get(userId));
    }

    /**
     * Updates the tier of an existing user, with an optional expiry timestamp.
     *
     * @param userId     The ID of the user to update.
     * @param tier       The new tier to assign (FREE, PRO, ENTERPRISE, UNLIMITED).
     * @param expiresAt  Optional LocalDateTime when this tier should expire.
     *                   Pass null for a non-expiring tier assignment.
     * @return The updated user object.
     * @throws ResourceNotFoundException if no user is found with the given ID.
     */
    public User updateTier(String userId, String tier, LocalDateTime expiresAt) {
        User user = userStore.get(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        user.setTier(tier.toUpperCase());
        user.setTierExpiresAt(expiresAt);
        return user;
    }

    /**
     * Retrieves all registered users in the system.
     *
     * @return A collection of all User objects.
     */
    public Collection<User> findAll() {
        return userStore.values();
    }
}
