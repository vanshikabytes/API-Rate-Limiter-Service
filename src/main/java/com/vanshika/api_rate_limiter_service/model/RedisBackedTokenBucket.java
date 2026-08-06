package com.vanshika.api_rate_limiter_service.model;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

public class RedisBackedTokenBucket {
    private final String key;
    private final String bucketKey;
    private final long capacity;
    private final long refillTokens;
    private final long windowSeconds;
    private final RedisTemplate<String, String> redisTemplate;

    private static final RedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>(
        "local bucketKey = KEYS[1]\n" +
        "local reservationKey = KEYS[2]\n" +
        "local now = tonumber(ARGV[1])\n" +
        "local capacity = tonumber(ARGV[2])\n" +
        "local refillTokens = tonumber(ARGV[3])\n" +
        "local windowSeconds = tonumber(ARGV[4])\n" +
        "local tokensToConsume = tonumber(ARGV[5])\n" +
        "local userId = ARGV[6]\n" +
        "local createdAt = ARGV[7]\n" +
        "\n" +
        "local state = redis.call('HMGET', bucketKey, 'tokens', 'last_refill_time_ms')\n" +
        "local availableTokens = tonumber(state[1])\n" +
        "local lastRefillTime = tonumber(state[2])\n" +
        "\n" +
        "if not availableTokens then\n" +
        "    availableTokens = capacity\n" +
        "    lastRefillTime = now\n" +
        "else\n" +
        "    local timeElapsed = now - lastRefillTime\n" +
        "    local tokensToAdd = (windowSeconds > 0) and math.floor((timeElapsed * refillTokens) / (windowSeconds * 1000)) or 0\n" +
        "    if tokensToAdd > 0 then\n" +
        "        availableTokens = math.min(capacity, availableTokens + tokensToAdd)\n" +
        "        lastRefillTime = now\n" +
        "    end\n" +
        "end\n" +
        "\n" +
        "if availableTokens >= tokensToConsume then\n" +
        "    availableTokens = availableTokens - tokensToConsume\n" +
        "    \n" +
        "    redis.call('HMSET', bucketKey, 'tokens', tostring(availableTokens), 'last_refill_time_ms', tostring(lastRefillTime), 'capacity', tostring(capacity))\n" +
        "    if windowSeconds > 0 then\n" +
        "        redis.call('EXPIRE', bucketKey, windowSeconds * 2)\n" +
        "    end\n" +
        "    \n" +
        "    local metadata = '{\"user\":\"' .. userId .. '\", \"createdAt\":\"' .. createdAt .. '\", \"tokensReserved\":' .. tostring(tokensToConsume) .. '}'\n" +
        "    redis.call('SET', reservationKey, metadata, 'EX', 60)\n" +
        "    \n" +
        "    return 1\n" +
        "end\n" +
        "\n" +
        "return 0",
        Long.class
    );

    private static final RedisScript<Long> COMMIT_SCRIPT = new DefaultRedisScript<>(
        "local reservationKey = KEYS[1]\n" +
        "if redis.call('EXISTS', reservationKey) == 1 then\n" +
        "    redis.call('DEL', reservationKey)\n" +
        "    return 1\n" +
        "end\n" +
        "return 0",
        Long.class
    );

    private static final RedisScript<Long> ROLLBACK_SCRIPT = new DefaultRedisScript<>(
        "local bucketKey = KEYS[1]\n" +
        "local reservationKey = KEYS[2]\n" +
        "local tokensToRefund = tonumber(ARGV[1])\n" +
        "if redis.call('EXISTS', reservationKey) == 1 then\n" +
        "    redis.call('DEL', reservationKey)\n" +
        "    local availableTokens = tonumber(redis.call('HGET', bucketKey, 'tokens'))\n" +
        "    if availableTokens then\n" +
        "        local capacity = tonumber(redis.call('HGET', bucketKey, 'capacity'))\n" +
        "        if capacity then\n" +
        "            availableTokens = math.min(capacity, availableTokens + tokensToRefund)\n" +
        "        else\n" +
        "            availableTokens = availableTokens + tokensToRefund\n" +
        "        end\n" +
        "        redis.call('HSET', bucketKey, 'tokens', tostring(availableTokens))\n" +
        "    end\n" +
        "    return 1\n" +
        "end\n" +
        "return 0",
        Long.class
    );

    public RedisBackedTokenBucket(String key, long capacity, long refillTokens, long windowSeconds, RedisTemplate<String, String> redisTemplate) {
        this.key = key;
        // Use Hash Tags to ensure bucket and reservations land on the same slot
        this.bucketKey = "rate_limit:{" + key + "}";
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.windowSeconds = windowSeconds;
        this.redisTemplate = redisTemplate;
    }

    public boolean tryConsume(int tokensToConsume) {
        throw new UnsupportedOperationException("Phase 1 tryConsume is deprecated. Use tryReserve, commit, and rollback.");
    }

    public boolean tryReserve(int tokensToConsume, String reservationId) {
        long now = Instant.now().toEpochMilli();
        String reservationKey = "reservation:{" + key + "}:" + reservationId;
        String createdAt = Instant.now().toString();
        
        Long result = redisTemplate.execute(
            RESERVE_SCRIPT,
            Arrays.asList(bucketKey, reservationKey),
            String.valueOf(now),
            String.valueOf(capacity),
            String.valueOf(refillTokens),
            String.valueOf(windowSeconds),
            String.valueOf(tokensToConsume),
            key,
            createdAt
        );
        return result != null && result == 1L;
    }

    public boolean commit(String reservationId) {
        String reservationKey = "reservation:{" + key + "}:" + reservationId;
        Long result = redisTemplate.execute(
            COMMIT_SCRIPT,
            Collections.singletonList(reservationKey)
        );
        return result != null && result == 1L;
    }

    public boolean rollback(int tokensToRefund, String reservationId) {
        String reservationKey = "reservation:{" + key + "}:" + reservationId;
        Long result = redisTemplate.execute(
            ROLLBACK_SCRIPT,
            Arrays.asList(bucketKey, reservationKey),
            String.valueOf(tokensToRefund)
        );
        return result != null && result == 1L;
    }

    public long getRemainingTokens() {
        long now = Instant.now().toEpochMilli();
        String tokensStr = (String) redisTemplate.opsForHash().get(bucketKey, "tokens");
        String lastRefillStr = (String) redisTemplate.opsForHash().get(bucketKey, "last_refill_time_ms");

        long availableTokens = (tokensStr != null) ? Long.parseLong(tokensStr) : capacity;
        long lastRefillTime = (lastRefillStr != null) ? Long.parseLong(lastRefillStr) : now;

        long timeElapsed = now - lastRefillTime;
        long tokensToAdd = (windowSeconds > 0) ? (timeElapsed * refillTokens) / (windowSeconds * 1000) : 0;

        return Math.min(capacity, availableTokens + tokensToAdd);
    }

    public long getSecondsUntilRefill() {
        long now = Instant.now().toEpochMilli();
        String tokensStr = (String) redisTemplate.opsForHash().get(bucketKey, "tokens");
        String lastRefillStr = (String) redisTemplate.opsForHash().get(bucketKey, "last_refill_time_ms");

        long availableTokens = (tokensStr != null) ? Long.parseLong(tokensStr) : capacity;
        long lastRefillTime = (lastRefillStr != null) ? Long.parseLong(lastRefillStr) : now;

        long timeElapsed = now - lastRefillTime;
        long tokensToAdd = (windowSeconds > 0) ? (timeElapsed * refillTokens) / (windowSeconds * 1000) : 0;
        long currentTokens = Math.min(capacity, availableTokens + tokensToAdd);

        if (currentTokens >= 1) {
            return 0;
        }

        if (refillTokens <= 0 || windowSeconds <= 0) {
            return 0;
        }

        long msNeeded = (windowSeconds * 1000) / refillTokens;
        long msRemaining = msNeeded - timeElapsed;
        if (msRemaining <= 0) {
            return 0;
        }
        return (long) Math.ceil(msRemaining / 1000.0);
    }
}

