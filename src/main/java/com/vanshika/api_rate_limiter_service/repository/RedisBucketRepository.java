package com.vanshika.api_rate_limiter_service.repository;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Repository
public class RedisBucketRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<List> script;

    public RedisBucketRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>();
        this.script.setResultType(List.class);
        this.script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/rate_limiter.lua")));
    }

    /**
     * Tries to consume tokens from a Redis bucket.
     * 
     * @param key           Redis key
     * @param capacity      Maximum capacity
     * @param refillRate    Tokens refilled per window
     * @param windowSeconds Duration of the window in seconds
     * @param requested     Tokens requested
     * @return List containing [isAllowed (1 or 0), remainingTokens]
     */
    public List<Long> tryConsume(String key, long capacity, long refillRate, long windowSeconds, int requested) {
        return (List<Long>) redisTemplate.execute(
                script,
                Collections.singletonList("rate_limit:" + key),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(windowSeconds),
                String.valueOf(Instant.now().getEpochSecond()),
                String.valueOf(requested)
        );
    }

    public void removeBucket(String key) {
        redisTemplate.delete("rate_limit:" + key);
    }
}
