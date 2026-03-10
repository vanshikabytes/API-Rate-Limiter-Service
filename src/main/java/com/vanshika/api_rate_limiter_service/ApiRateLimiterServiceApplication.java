package com.vanshika.api_rate_limiter_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiRateLimiterServiceApplication {
	// main class
	public static void main(String[] args) {
		SpringApplication.run(ApiRateLimiterServiceApplication.class, args);
	}

}
