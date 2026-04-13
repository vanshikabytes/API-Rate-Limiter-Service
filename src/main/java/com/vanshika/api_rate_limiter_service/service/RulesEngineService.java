package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.model.RateLimitRule;
import com.vanshika.api_rate_limiter_service.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service responsible for matching incoming requests against defined rate limit rules.
 */
@Service
@RequiredArgsConstructor
public class RulesEngineService {

    private final RuleRepository ruleRepository;

    /**
     * Finds the first active rule that matches the request path and method.
     * Rules are evaluated in priority order.
     */
    public Optional<RateLimitRule> resolve(String path, String method) {
        return ruleRepository.findAll().stream()
                .filter(rule -> rule.matches(path, method))
                .filter(RateLimitRule::isActiveNow)
                .findFirst();
    }
}
