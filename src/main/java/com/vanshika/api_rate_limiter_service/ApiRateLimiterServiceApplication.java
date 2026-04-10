package com.vanshika.api_rate_limiter_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the API Rate Limiter Service.
 * 
 * @EnableScheduling is required to trigger periodic cleanup of expired token 
 * buckets in the repository layer.
 */
@SpringBootApplication
@EnableScheduling
public class ApiRateLimiterServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiRateLimiterServiceApplication.class, args);
	}
}
