package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.config.RateLimiterProperties;
import com.vanshika.api_rate_limiter_service.exception.InvalidKeyException;
import com.vanshika.api_rate_limiter_service.model.RateLimitStatus;
import com.vanshika.api_rate_limiter_service.model.TokenBucket;
import com.vanshika.api_rate_limiter_service.repository.InMemoryBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RateLimiterService.
 */
class RateLimiterServiceTest {

    private RateLimiterService service;

    @BeforeEach
    void setup() {
        InMemoryBucketRepository repository = new InMemoryBucketRepository();
        com.vanshika.api_rate_limiter_service.repository.UserRepository userRepository = new com.vanshika.api_rate_limiter_service.repository.UserRepository();
        com.vanshika.api_rate_limiter_service.config.properties.TierConfig tierConfig = new com.vanshika.api_rate_limiter_service.config.properties.TierConfig();

        RateLimiterProperties properties = new RateLimiterProperties();
        RateLimiterProperties.RateLimitConfig defaultConfig = new RateLimiterProperties.RateLimitConfig();
        defaultConfig.setCapacity(1);
        defaultConfig.setRefillTokens(1); // refill 1 token every 60 seconds
        defaultConfig.setWindowSeconds(60);

        properties.setIp(defaultConfig); // Set fallback limit
        properties.setUser(defaultConfig); // Set user limit

        service = new RateLimiterService(repository, properties, userRepository, tierConfig);
    }

    @Test
    void shouldBlockAfterCapacityReached() {
        TokenBucket bucket = service.getBucket("user:1");

        assertTrue(bucket.tryConsume(),  "First request should be allowed");
        assertFalse(bucket.tryConsume(), "Second request should be blocked (capacity = 1)");
    }

    @Test
    void shouldReturnCorrectStatus() {
        String uuid1 = UUID.randomUUID().toString();
        RateLimitStatus status = service.reserveToken("user:1", uuid1);

        assertTrue(status.isAllowed(), "First request should be allowed");
        assertEquals(0, status.getRemainingTokens(), "Tokens should be exhausted");
        assertEquals(1, status.getCapacity());
        assertTrue(status.getResetSeconds() >= 1, "Reset seconds should be at least 1");

        String uuid2 = UUID.randomUUID().toString();
        RateLimitStatus blockedStatus = service.reserveToken("user:1", uuid2);
        assertFalse(blockedStatus.isAllowed(), "Second request should be blocked");
    }

    @Test
    void shouldResetBucket() {
        String uuid = UUID.randomUUID().toString();
        service.reserveToken("user:1", uuid);
        service.reset("user:1");

        RateLimitStatus afterReset = service.reserveToken("user:1", UUID.randomUUID().toString());
        assertTrue(afterReset.isAllowed(), "Request after reset should be allowed");
    }

    @Test
    void shouldThrowWhenKeyIsNull() {
        assertThrows(InvalidKeyException.class,
                () -> service.reserveToken(null, UUID.randomUUID().toString()));
    }

    @Test
    void shouldThrowWhenKeyIsBlank() {
        assertThrows(InvalidKeyException.class,
                () -> service.reserveToken("   ", UUID.randomUUID().toString()));
    }
}