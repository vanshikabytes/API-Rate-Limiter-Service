package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.model.RateRule;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Comparator;
import java.util.Optional;

@Service
public class RuleEngineService {

    private final RateLimiterProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RuleEngineService(RateLimiterProperties properties) {
        this.properties = properties;
    }

    public Optional<RateRule> match(String path, String method) {
        return properties.getRules().stream()
                .filter(rule -> matchesPath(rule.getPath(), path))
                .filter(rule -> matchesMethod(rule.getMethod(), method))
                .max(Comparator.comparingInt(RateRule::getPriority));
    }

    private boolean matchesPath(String pattern, String path) {
        return pathMatcher.match(pattern, path);
    }

    private boolean matchesMethod(HttpMethod ruleMethod, String requestMethod) {
        if (ruleMethod == null) return true; // null means all methods
        return ruleMethod.name().equalsIgnoreCase(requestMethod);
    }
}
