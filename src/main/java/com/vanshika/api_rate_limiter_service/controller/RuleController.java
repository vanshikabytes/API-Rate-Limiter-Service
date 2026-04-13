package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import com.vanshika.api_rate_limiter_service.model.RateLimitRule;
import com.vanshika.api_rate_limiter_service.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing Dynamic Rate Limit Rules.
 */
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleRepository ruleRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> addRule(@RequestBody RateLimitRule rule) {
        ruleRepository.addRule(rule);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rule added successfully: " + rule.getId(), rule.getId()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RateLimitRule>>> getAllRules() {
        List<RateLimitRule> rules = ruleRepository.findAll();
        return ResponseEntity.ok(new ApiResponse<>(true, "Rules retrieved successfully", rules));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> removeRule(@PathVariable String id) {
        ruleRepository.removeRule(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rule removed successfully: " + id, id));
    }
}
