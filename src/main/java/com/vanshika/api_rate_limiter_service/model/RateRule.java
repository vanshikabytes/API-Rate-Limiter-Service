package com.vanshika.api_rate_limiter_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateRule {
    private String name;
    private String path; // e.g., /api/search/**
    private HttpMethod method; // ALL, GET, POST, etc.
    private long capacity;
    private long refillRate;
    private TimeWindow window;
    private int priority; // Higher number = higher priority
}
