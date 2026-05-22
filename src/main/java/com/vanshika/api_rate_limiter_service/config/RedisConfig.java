package com.vanshika.api_rate_limiter_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis connection and template configuration.
 *
 * Only loaded when rate-limiter.storage=redis.
 * Lettuce is the Spring Boot default Redis client — non-blocking, thread-safe,
 * and connection-pooling friendly.
 *
 * Configuration values are read from:
 *   spring.data.redis.host  (default: localhost)
 *   spring.data.redis.port  (default: 6379)
 */
@Configuration
@ConditionalOnProperty(name = "rate-limiter.storage", havingValue = "redis")
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    /**
     * Creates a Lettuce-backed connection factory pointing at the configured Redis host.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    /**
     * StringRedisTemplate serialises keys and values as plain UTF-8 strings.
     * This makes the Redis data human-readable in redis-cli, which is useful
     * during demos: HGETALL rate_limit:user:alice shows all bucket fields.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
