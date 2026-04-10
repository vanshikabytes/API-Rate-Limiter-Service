package com.vanshika.api_rate_limiter_service.config;

import com.vanshika.api_rate_limiter_service.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global Web configurations.
 * 
 * We implement WebMvcConfigurer to register our RateLimitInterceptor.
 * This ensures the rate limit logic runs automatically for incoming requests
 * before they reached the controller.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    /**
     * Wires the interceptor to specific URL paths.
     * We only protect '/api/backend/**' to allow administrative and health check 
     * endpoints to remain accessible even when rate limits are hit.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/backend/**");
    }
}
