package com.vanshika.api_rate_limiter_service;

import com.vanshika.api_rate_limiter_service.config.properties.TierConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the API Rate Limiter Service.
 * 
 * @EnableScheduling is required to trigger periodic cleanup of expired token
 *                   buckets in the repository layer.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(TierConfig.class) // NEW: Register TierConfig for hierarchical limits
public class ApiRateLimiterServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiRateLimiterServiceApplication.class, args);
	}
}
