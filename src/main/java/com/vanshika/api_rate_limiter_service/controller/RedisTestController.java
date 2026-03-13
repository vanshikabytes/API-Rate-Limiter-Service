package com.vanshika.api_rate_limiter_service.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/redis")
public class RedisTestController {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTestController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> response = new HashMap<>();
        try {
            String pong = (String) redisTemplate.getConnectionFactory().getConnection().ping();
            response.put("status", "UP");
            response.put("message", "Redis connection successful");
            response.put("ping", pong);
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("message", "Redis connection failed: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/set")
    public String set(@RequestParam String key, @RequestParam String value) {
        redisTemplate.opsForValue().set(key, value);
        return "Set " + key + "=" + value;
    }

    @GetMapping("/get")
    public Object get(@RequestParam String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
