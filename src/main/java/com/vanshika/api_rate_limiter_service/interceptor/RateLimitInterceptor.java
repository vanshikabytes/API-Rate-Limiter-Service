package com.vanshika.api_rate_limiter_service.interceptor;

import com.vanshika.api_rate_limiter_service.exception.RateLimitExceededException;
import com.vanshika.api_rate_limiter_service.model.RateLimitRule;
import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import com.vanshika.api_rate_limiter_service.service.RulesEngineService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Intercepts incoming requests to enforce rate limits before they reach the controller.
 *
 * It first checks the Advanced Rules Engine for any matching path/method/time constraints.
 * If a rule matches, it applies those specific limits. Otherwise, it falls back
 * to the default Redis-backed user/IP rate limiting.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final RulesEngineService rulesEngineService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService, RulesEngineService rulesEngineService) {
        this.rateLimiterService = rateLimiterService;
        this.rulesEngineService = rulesEngineService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws Exception {

        // 1. Identify client
        String userId = request.getHeader("X-User-Id");
        String key = (userId != null && !userId.isBlank()) ? "user:" + userId : "ip:" + request.getRemoteAddr();

        // 2. Resolve rules (Endpoint + Method + Time matching)
        String path = request.getRequestURI();
        String method = request.getMethod();
        Optional<RateLimitRule> ruleMatch = rulesEngineService.resolve(path, method);

        RateLimitStatus status;

        if (ruleMatch.isPresent()) {
            // APPLY RULE: Use a specific key for the rule (e.g. user:123:rule:id)
            RateLimitRule rule = ruleMatch.get();
            String ruleKey = key + ":rule:" + rule.getId();
            status = rateLimiterService.getRateLimitStatusWithOverride(
                    ruleKey,
                    rule.getCapacity(),
                    rule.getRefillTokens(),
                    rule.getWindowSeconds()
            );
        } else {
            // FALLBACK: default behavior
            status = rateLimiterService.getRateLimitStatus(key);
        }

        // 3. Set standard headers
        response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemainingTokens()));
        response.setHeader("X-RateLimit-Capacity",  String.valueOf(status.getCapacity()));
        response.setHeader("X-RateLimit-Reset",     String.valueOf(status.getResetSeconds()));

        if (!status.isAllowed()) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Try again in " + status.getResetSeconds() + " second(s).",
                    status.getRemainingTokens(),
                    status.getCapacity(),
                    status.getResetSeconds()
            );
        }

        return true; 
    }
}
