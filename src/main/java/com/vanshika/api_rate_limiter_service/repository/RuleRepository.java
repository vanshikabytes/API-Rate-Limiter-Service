package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.RateLimitRule;
import java.util.List;

/**
 * Base repository interface for managing dynamic rate limit rules.
 * Implementations can store rules in-memory or in a distributed store like Redis.
 */
public interface RuleRepository {
    /**
     * Persists a new rule or updates an existing one.
     */
    void addRule(RateLimitRule rule);

    /**
     * Retrieves all defined rules, typically sorted by priority.
     */
    List<RateLimitRule> findAll();

    /**
     * Removes a rule by its unique identifier.
     */
    void removeRule(String id);
}
