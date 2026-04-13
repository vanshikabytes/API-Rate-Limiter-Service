package com.vanshika.api_rate_limiter_service.integration;

import com.vanshika.api_rate_limiter_service.model.RateLimitRule;
import com.vanshika.api_rate_limiter_service.repository.RuleRepository;
import com.vanshika.api_rate_limiter_service.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * CONCURRENCY VALIDATION TEST
 * 
 * This test proves the atomicity of our distributed rate limiter.
 * By firing many parallel requests at once, we verify that the Redis Lua script
 * correctly enforces the capacity limit without race conditions.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class RateLimiterConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConcurrencyTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private RuleRepository ruleRepository;

    private static final String TEST_USER_ID = "test-concurrency-user";
    private static final String ENDPOINT = "/api/backend/employees";

    @BeforeEach
    void setup() {
        // Ensure a clean state before each test run
        rateLimiterService.reset("user:" + TEST_USER_ID);
        // Clear any existing rules to avoid interference
        ruleRepository.findAll().forEach(rule -> ruleRepository.removeRule(rule.getId()));
    }

    @Test
    @DisplayName("Verify concurrency for default rate limits (Atomic verification)")
    void testConcurrencyWithDefaultLimits() throws Exception {
        // GIVEN: Default capacity is 5 tokens (from application.yaml)
        int capacity = 5;
        int totalRequests = 50;
        int threadCount = 10;

        // WHEN: Execution happens in parallel
        TestResult result = runParallelLoad(totalRequests, threadCount, TEST_USER_ID, ENDPOINT);

        // THEN: Exactly 'capacity' requests should succeed (200 OK)
        // And the rest should be rejected (429 Too Many Requests)
        log.info("Default Concurrency Result -> Allowed: {}, Blocked: {}", result.successCount, result.failure429Count);
        
        assertEquals(capacity, result.successCount.get(), "Should allow exactly " + capacity + " requests");
        assertEquals(totalRequests - capacity, result.failure429Count.get(), "Remaining should be 429");
    }

    @Test
    @DisplayName("Verify concurrency with Advanced Rules Engine override")
    void testConcurrencyWithRuleOverride() throws Exception {
        // GIVEN: A high-priority rule restricting this endpoint to exactly 3 requests
        int ruleCapacity = 3;
        RateLimitRule rule = RateLimitRule.builder()
                .id("critical-concurrency-test")
                .pathPattern(ENDPOINT)
                .priority(1)
                .capacity(ruleCapacity)
                .refillTokens(ruleCapacity)
                .windowSeconds(60)
                .build();
        ruleRepository.addRule(rule);

        int totalRequests = 20;
        int threadCount = 10;

        // WHEN: Parallel load hits the protected endpoint
        TestResult result = runParallelLoad(totalRequests, threadCount, TEST_USER_ID, ENDPOINT);

        // THEN: The rule should take precedence over default limits
        log.info("Rule Concurrency Result -> Allowed: {}, Blocked: {}", result.successCount, result.failure429Count);

        assertEquals(ruleCapacity, result.successCount.get(), "Rule should strictly enforce capacity of " + ruleCapacity);
        assertEquals(totalRequests - ruleCapacity, result.failure429Count.get(), "All overflow must be 429");
    }

    /**
     * Helper to run parallel requests using ExecutorService and CountDownLatch.
     */
    private TestResult runParallelLoad(int totalRequests, int threads, String userId, String path) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failure429Count = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for the whistle
                    MvcResult mvcResult = mockMvc.perform(get(path)
                            .header("X-User-Id", userId))
                            .andReturn();

                    int status = mvcResult.getResponse().getStatus();
                    if (status == 200) {
                        successCount.incrementAndGet();
                    } else if (status == 429) {
                        failure429Count.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Request failed: {}", e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Blast off!
        finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        return new TestResult(successCount, failure429Count);
    }

    private static class TestResult {
        final AtomicInteger successCount;
        final AtomicInteger failure429Count;

        TestResult(AtomicInteger success, AtomicInteger failure) {
            this.successCount = success;
            this.failure429Count = failure;
        }
    }
}
