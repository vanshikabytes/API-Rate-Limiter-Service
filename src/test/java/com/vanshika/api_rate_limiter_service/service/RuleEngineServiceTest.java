package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.model.RateRule;
import com.vanshika.api_rate_limiter_service.model.TimeWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineServiceTest {

    private RuleEngineService ruleEngine;
    private RateLimiterProperties properties;

    @BeforeEach
    void setup() {
        properties = new RateLimiterProperties();
        
        RateRule rule1 = RateRule.builder()
                .name("search")
                .path("/api/search/**")
                .method(HttpMethod.GET)
                .capacity(10)
                .priority(1)
                .window(TimeWindow.MINUTE)
                .build();

        RateRule rule2 = RateRule.builder()
                .name("search-specific")
                .path("/api/search/fast")
                .method(HttpMethod.GET)
                .capacity(5)
                .priority(10) // Higher priority
                .window(TimeWindow.MINUTE)
                .build();

        properties.setRules(List.of(rule1, rule2));
        ruleEngine = new RuleEngineService(properties);
    }

    @Test
    void shouldMatchSpecificRuleOverGeneric() {
        Optional<RateRule> match = ruleEngine.match("/api/search/fast", "GET");
        assertTrue(match.isPresent());
        assertEquals("search-specific", match.get().getName());
    }

    @Test
    void shouldMatchGenericRule() {
        Optional<RateRule> match = ruleEngine.match("/api/search/something", "GET");
        assertTrue(match.isPresent());
        assertEquals("search", match.get().getName());
    }

    @Test
    void shouldNotMatchWrongMethod() {
        Optional<RateRule> match = ruleEngine.match("/api/search/fast", "POST");
        assertFalse(match.isPresent());
    }

    @Test
    void shouldNotMatchWrongPath() {
        Optional<RateRule> match = ruleEngine.match("/api/other", "GET");
        assertFalse(match.isPresent());
    }
}
