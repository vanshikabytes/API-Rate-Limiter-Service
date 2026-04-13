package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.RateLimitRule;
import com.vanshika.api_rate_limiter_service.repository.RuleRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the RuleRepository interface.
 * Uses a ConcurrentHashMap for thread-safe rule storage.
 */
@Repository
public class InMemoryRuleRepository implements RuleRepository {

    private final Map<String, RateLimitRule> ruleStore = new ConcurrentHashMap<>();

    @Override
    public void addRule(RateLimitRule rule) {
        ruleStore.put(rule.getId(), rule);
    }

    @Override
    public List<RateLimitRule> findAll() {
        return ruleStore.values().stream()
                .sorted(Comparator.comparingInt(RateLimitRule::getPriority))
                .collect(Collectors.toList());
    }

    @Override
    public void removeRule(String id) {
        ruleStore.remove(id);
    }
}
