package com.vanshika.api_rate_limiter_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis configuration.
 *
 * We use StringRedisTemplate (not the generic RedisTemplate) because all
 * values we store — token counts, timestamps — are simple ASCII numerics.
 * This avoids Java-serialization overhead and keeps Redis keys human-readable,
 * which is important for debugging distributed state.
 *
 * Lua scripts are pre-loaded as Spring beans here so they are compiled once
 * at startup, not on every request.
 */
@Configuration
public class RedisConfig {

    /**
     * StringRedisTemplate backed by the auto-configured connection factory
     * (Lettuce by default when spring-boot-starter-data-redis is on the classpath).
     *
     * Lettuce is non-blocking and multiplexes multiple threads over a single
     * TCP connection, which is ideal for a high-throughput rate-limiter.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lua Scripts — compiled once, reused per request
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * CONSUME script — atomically refills + decrements the bucket.
     *
     * KEYS[1] = rate_limit:{key}:tokens
     * KEYS[2] = rate_limit:{key}:timestamp
     *
     * ARGV[1] = capacity        (long)
     * ARGV[2] = refillRate      (double, tokens/second)
     * ARGV[3] = nowMs           (current epoch in milliseconds, as string)
     * ARGV[4] = windowSeconds   (TTL for auto-cleanup)
     *
     * Returns: { allowed (1|0), remaining (long) }
     *
     * Why Lua?
     * Redis executes Lua scripts atomically — no other command runs between
     * the GET and SET. This eliminates the race condition where two instances
     * could both read "1 token available" and both allow the request.
     */
    @Bean(name = "consumeTokenScript")
    public DefaultRedisScript<java.util.List> consumeTokenScript() {
        DefaultRedisScript<java.util.List> script = new DefaultRedisScript<>();
        script.setResultType(java.util.List.class);
        script.setScriptText(
            // language=Lua
            "local tokens_key    = KEYS[1]\n" +
            "local ts_key        = KEYS[2]\n" +
            "local capacity      = tonumber(ARGV[1])\n" +
            "local refill_rate   = tonumber(ARGV[2])\n" +
            "local now_ms        = tonumber(ARGV[3])\n" +
            "local window_secs   = tonumber(ARGV[4])\n" +
            "\n" +
            "-- Read existing state\n" +
            "local stored_tokens = tonumber(redis.call('GET', tokens_key))\n" +
            "local stored_ts_ms  = tonumber(redis.call('GET', ts_key))\n" +
            "\n" +
            "-- First-time initialization\n" +
            "if stored_tokens == nil then\n" +
            "    stored_tokens = capacity\n" +
            "    stored_ts_ms  = now_ms\n" +
            "end\n" +
            "\n" +
            "-- Continuous refill: tokens earned since last call\n" +
            "local elapsed_secs  = (now_ms - stored_ts_ms) / 1000.0\n" +
            "local tokens_to_add = elapsed_secs * refill_rate\n" +
            "local tokens        = math.min(capacity, stored_tokens + tokens_to_add)\n" +
            "\n" +
            "-- Try to consume one token\n" +
            "local allowed   = 0\n" +
            "local remaining = math.floor(tokens)\n" +
            "if tokens >= 1 then\n" +
            "    tokens    = tokens - 1\n" +
            "    allowed   = 1\n" +
            "    remaining = math.floor(tokens)\n" +
            "end\n" +
            "\n" +
            "-- Persist updated state with TTL (auto-cleanup after inactivity)\n" +
            "redis.call('SET', tokens_key, tostring(tokens))\n" +
            "redis.call('SET', ts_key,     tostring(now_ms))\n" +
            "redis.call('EXPIRE', tokens_key, window_secs)\n" +
            "redis.call('EXPIRE', ts_key,     window_secs)\n" +
            "\n" +
            "return {allowed, remaining}\n"
        );
        return script;
    }

    /**
     * STATUS script — reads and refills locally WITHOUT decrementing.
     * Used by the /status endpoint so inspection never consumes a token.
     *
     * KEYS / ARGV: same contract as the consume script.
     * Returns: { remaining (long), resetSeconds (long) }
     */
    @Bean(name = "statusScript")
    public DefaultRedisScript<java.util.List> statusScript() {
        DefaultRedisScript<java.util.List> script = new DefaultRedisScript<>();
        script.setResultType(java.util.List.class);
        script.setScriptText(
            // language=Lua
            "local tokens_key    = KEYS[1]\n" +
            "local ts_key        = KEYS[2]\n" +
            "local capacity      = tonumber(ARGV[1])\n" +
            "local refill_rate   = tonumber(ARGV[2])\n" +
            "local now_ms        = tonumber(ARGV[3])\n" +
            "local window_secs   = tonumber(ARGV[4])\n" +
            "\n" +
            "local stored_tokens = tonumber(redis.call('GET', tokens_key))\n" +
            "local stored_ts_ms  = tonumber(redis.call('GET', ts_key))\n" +
            "\n" +
            "if stored_tokens == nil then\n" +
            "    return {capacity, 0}\n" +
            "end\n" +
            "\n" +
            "local elapsed_secs = (now_ms - stored_ts_ms) / 1000.0\n" +
            "local tokens = math.min(capacity, stored_tokens + elapsed_secs * refill_rate)\n" +
            "\n" +
            "local remaining    = math.floor(tokens)\n" +
            "local reset_secs   = 0\n" +
            "if tokens < 1 and refill_rate > 0 then\n" +
            "    reset_secs = math.ceil((1 - tokens) / refill_rate)\n" +
            "end\n" +
            "\n" +
            "return {remaining, reset_secs}\n"
        );
        return script;
    }
}
