package com.vanshika.api_rate_limiter_service.config;

import com.vanshika.api_rate_limiter_service.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WEB MVC CONFIGURATION — Step 4: Registering Middleware
 *
 * ─────────────────────────────────────────────────────
 * What does this class do?
 * ─────────────────────────────────────────────────────
 * WebMvcConfigurer is Spring's hook for customizing the MVC framework.
 * By overriding addInterceptors(), we tell Spring to run our
 * RateLimitInterceptor for specific URL patterns BEFORE any controller
 * method is invoked.
 *
 * This is how middleware is "wired in" to the Spring request pipeline.
 * ─────────────────────────────────────────────────────
 * Why limit only to "/api/backend/**"?
 * ─────────────────────────────────────────────────────
 * - The /api/rate-limit/** endpoints (status, reset, health) are
 *   administrative endpoints — they should NOT be rate-limited themselves.
 * - Only the "protected" backend routes need the middleware guard.
 * - This separation is a real-world best practice (e.g., health checks
 *   must always be reachable even under throttle).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    /**
     * Register the RateLimitInterceptor for all /api/backend/** routes.
     *
     * addPathPatterns() — specifies which URLs the interceptor applies to.
     * excludePathPatterns() — can be used to whitelist specific sub-paths
     *                         (e.g., exclude a public health-check inside /api/backend/).
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/backend/**"); // Protect all backend routes
    }
}
