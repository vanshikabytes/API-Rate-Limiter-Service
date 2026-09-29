package com.vanshika.api_rate_limiter_service.service;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AuditLogService {
    private static final String AUDIT_LOG_KEY = "access:audit_log";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuditLogService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Asynchronously pushes an audit log entry to the head of the Redis List.
     */
    @Async("applicationTaskExecutor")
    public void logAction(String targetUserId, String action, String performedBy, String reason, String expiresAt) {
        log.trace("Background audit thread picked up log task for user: {}", targetUserId);
        try {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("action", action);
            logEntry.put("targetUserId", targetUserId);
            logEntry.put("performedBy", performedBy);
            logEntry.put("reason", reason != null && !reason.isBlank() ? reason : "No reason provided");
            logEntry.put("timestamp", Instant.now().toString());

            if (expiresAt != null) {
                logEntry.put("expiresAt", expiresAt);
            }

            String jsonPayload = objectMapper.writeValueAsString(logEntry);
            log.debug("Audit log payload ready: {}", jsonPayload);

            // LPUSH adds the JSON string to the head of the list (index 0)
            redisTemplate.opsForList().leftPush(AUDIT_LOG_KEY, jsonPayload);

            log.info("Audit log saved for user '{}' — action: {}, performed by: {}", targetUserId, action, performedBy);

        } catch (JsonProcessingException e) {
            log.error("Could not serialize the audit log entry for user '{}', skipping log entry", targetUserId, e);
        } catch (Exception e) {
            log.error("Something went wrong while pushing audit log to Redis for user '{}'", targetUserId, e);
        }
    }
}

