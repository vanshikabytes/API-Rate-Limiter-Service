package com.vanshika.api_rate_limiter_service.service;

import com.vanshika.api_rate_limiter_service.dto.AccessResponse;
import com.vanshika.api_rate_limiter_service.dto.BulkImportResponse;
import com.vanshika.api_rate_limiter_service.dto.PaginatedUsersResponse;
import com.vanshika.api_rate_limiter_service.exception.ResourceNotFoundException;
import com.vanshika.api_rate_limiter_service.model.AccessDecision;
import com.vanshika.api_rate_limiter_service.model.ListType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AccessControlService {
    private static final String KEY_PREFIX = "access:";

    private final StringRedisTemplate redisTemplate;
    private final AuditLogService auditLogService;

    public AccessControlService(StringRedisTemplate redisTemplate, AuditLogService auditLogService) {
        this.redisTemplate = redisTemplate;
        this.auditLogService = auditLogService;
    }

    private String buildKey(ListType listType, String userId) {
        return KEY_PREFIX + listType.name().toLowerCase() + ":" + userId;
    }

    public AccessDecision checkAccess(String userId) {
        log.trace("Running access check for userId: {}", userId);

        String blacklistKey = buildKey(ListType.BLACKLIST, userId);
        String whitelistKey = buildKey(ListType.WHITELIST, userId);
        log.debug("Checking Redis keys — blacklist: {}, whitelist: {}", blacklistKey, whitelistKey);

        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            log.debug("Found {} in the blacklist, returning BLOCKED", userId);
            return AccessDecision.BLOCKED;
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(whitelistKey))) {
            log.debug("Found {} in the whitelist, returning WHITELISTED", userId);
            return AccessDecision.WHITELISTED;
        }

        log.trace("No list entry found for {}, treating as NORMAL", userId);
        return AccessDecision.NORMAL;
    }

    public AccessResponse setUserAccess(String userId, ListType listType, Integer durationSeconds, String reason, String performedBy) {
        log.info("Admin '{}' is adding user '{}' to {}", performedBy, userId, listType.name());

        ListType oppositeList = (listType == ListType.BLACKLIST) ? ListType.WHITELIST : ListType.BLACKLIST;
        String oppositeKey = buildKey(oppositeList, userId);
        log.debug("Removing user '{}' from opposite list ({}) to enforce mutual exclusivity", userId, oppositeList.name());
        redisTemplate.delete(oppositeKey);

        String targetKey = buildKey(listType, userId);
        boolean isPermanent = (durationSeconds == null || durationSeconds <= 0);
        String expiresAtStr = null;

        if (isPermanent) {
            redisTemplate.opsForValue().set(targetKey, "true");
            log.debug("Stored {} as a permanent entry in Redis with key: {}", userId, targetKey);
        } else {
            redisTemplate.opsForValue().set(targetKey, "true", durationSeconds, TimeUnit.SECONDS);
            expiresAtStr = Instant.now().plusSeconds(durationSeconds).toString();
            log.debug("Stored {} with a TTL of {}s in Redis. Expires at: {}", userId, durationSeconds, expiresAtStr);
        }

        auditLogService.logAction(userId, "ADDED_TO_" + listType.name(), performedBy, reason, expiresAtStr);
        log.info("Successfully added '{}' to {}. Permanent: {}", userId, listType.name(), isPermanent);
        return new AccessResponse("SUCCESS", userId, listType.name(), isPermanent, expiresAtStr);
    }

    public AccessResponse getUserStatus(String userId) {
        log.debug("Looking up current status for userId: {}", userId);

        Long blacklistTtl = redisTemplate.getExpire(buildKey(ListType.BLACKLIST, userId), TimeUnit.SECONDS);
        if (blacklistTtl != null && blacklistTtl != -2) {
            log.debug("User '{}' is on the BLACKLIST. TTL from Redis: {}s", userId, blacklistTtl);
            return buildStatusResponse(userId, ListType.BLACKLIST, blacklistTtl);
        }

        Long whitelistTtl = redisTemplate.getExpire(buildKey(ListType.WHITELIST, userId), TimeUnit.SECONDS);
        if (whitelistTtl != null && whitelistTtl != -2) {
            log.debug("User '{}' is on the WHITELIST. TTL from Redis: {}s", userId, whitelistTtl);
            return buildStatusResponse(userId, ListType.WHITELIST, whitelistTtl);
        }

        log.debug("User '{}' is not on any list, status is NORMAL", userId);
        return new AccessResponse("NORMAL", userId, "NONE", true, null);
    }

    private AccessResponse buildStatusResponse(String userId, ListType listType, Long ttl) {
        boolean isPermanent = (ttl == -1);
        String expiresAtStr = (!isPermanent && ttl > 0) ? Instant.now().plusSeconds(ttl).toString() : null;
        return new AccessResponse("FOUND", userId, listType.name(), isPermanent, expiresAtStr);
    }

    public void removeUserAccess(String userId, ListType listType, String performedBy) {
        log.info("Admin '{}' is removing user '{}' from {}", performedBy, userId, listType.name());
        if (Boolean.TRUE.equals(redisTemplate.delete(buildKey(listType, userId)))) {
            log.debug("Successfully deleted Redis key for '{}' from {}", userId, listType.name());
            auditLogService.logAction(userId, "REMOVED_FROM_" + listType.name(), performedBy, "Manual deletion", null);
        } else {
            log.warn("Tried to remove '{}' from {} but no matching key was found in Redis", userId, listType.name());
            throw new ResourceNotFoundException("User " + userId + " not found on " + listType.name());
        }
    }

    /**
     * View All API: Uses SCAN to prevent blocking, and Redis Pipelining to batch TTL network calls.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public PaginatedUsersResponse getAllUsers(ListType listType, int count) {
        String matchPattern = buildKey(listType, "*");
        log.debug("Scanning Redis for keys matching pattern: {}", matchPattern);
        List<String> keys = new ArrayList<>();

        ScanOptions options = ScanOptions.scanOptions().match(matchPattern).count(count).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext() && keys.size() < count) {
                keys.add(cursor.next());
            }
        }

        log.debug("SCAN returned {} keys for list type {}", keys.size(), listType.name());

        if (keys.isEmpty()) {
            log.info("No users found on the {} list", listType.name());
            return new PaginatedUsersResponse(listType.name(), "0", new ArrayList<>());
        }

        log.debug("Fetching TTLs for {} keys using pipeline", keys.size());
        List<Object> ttlResults = redisTemplate.executePipelined(
                new org.springframework.data.redis.core.SessionCallback<Object>() {
                    @Override
                    public Object execute(org.springframework.data.redis.core.RedisOperations operations) {
                        for (String key : keys) {
                            operations.getExpire(key);
                        }
                        return null;
                    }
                });

        List<PaginatedUsersResponse.UserStatus> users = new ArrayList<>();
        String prefixToRemove = KEY_PREFIX + listType.name().toLowerCase() + ":";

        for (int i = 0; i < keys.size(); i++) {
            String rawKey = keys.get(i);
            String rawUserId = rawKey.startsWith(prefixToRemove)
                    ? rawKey.substring(prefixToRemove.length()) : rawKey;
            Long ttl = ttlResults.get(i) instanceof Long ? (Long) ttlResults.get(i) : null;
            log.trace("User: {}, TTL from Redis: {}", rawUserId, ttl);
            users.add(new PaginatedUsersResponse.UserStatus(rawUserId, (ttl == null || ttl == -1) ? null : ttl));
        }

        log.info("Returning {} users from {} list", users.size(), listType.name());
        return new PaginatedUsersResponse(listType.name(), keys.size() < count ? "0" : "next_page_token", users);
    }

    /**
     * Bulk Import API: Uses Stream Chunking (protects JVM) and Pipelining (protects Network).
     */
    public BulkImportResponse processBulkCsv(MultipartFile file, ListType listType, String performedBy) {
        log.info("Starting bulk CSV import for list: {} by admin: {}", listType.name(), performedBy);
        int total = 0, success = 0, failed = 0;
        
        StringBuilder errorCsvBuilder = new StringBuilder();
        List<String[]> currentChunk = new ArrayList<>();
        final int CHUNK_SIZE = 1000;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) { 
                    isHeader = false; 
                    errorCsvBuilder.append(line).append(",Error_Reason\n");
                    continue; 
                }
                
                String[] parts = line.split(",", -1);
                total++;
                
                String errorReason = null;
                if (parts.length < 1 || parts[0].trim().isEmpty()) {
                    errorReason = "Missing userId";
                } else if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    try {
                        int duration = Integer.parseInt(parts[1].trim());
                        if (duration <= 0) {
                            errorReason = "Duration must be positive";
                        }
                    } catch (NumberFormatException e) {
                        errorReason = "Invalid duration format";
                    }
                }
                
                if (errorReason != null) {
                    failed++;
                    errorCsvBuilder.append(line).append(",").append(errorReason).append("\n");
                    continue;
                }
                
                currentChunk.add(parts);

                if (currentChunk.size() >= CHUNK_SIZE) {
                    log.debug("Chunk of {} rows is ready, flushing to Redis via pipeline", currentChunk.size());
                    processChunkWithPipeline(currentChunk, listType, performedBy);
                    success += currentChunk.size();
                    currentChunk.clear();
                    log.trace("Chunk flushed and memory cleared, continuing to next chunk");
                }
            }

            if (!currentChunk.isEmpty()) {
                log.debug("Processing final chunk of {} remaining rows", currentChunk.size());
                processChunkWithPipeline(currentChunk, listType, performedBy);
                success += currentChunk.size();
            }
        } catch (Exception e) {
            log.error("CSV import failed unexpectedly while processing file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage() + " | Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));
        }

        byte[] errorCsvFile = null;
        if (failed > 0) {
            errorCsvFile = errorCsvBuilder.toString().getBytes();
        }

        String status = (failed == 0) ? "COMPLETED" : "COMPLETED_WITH_ERRORS";
        log.info("CSV import finished — total: {}, success: {}, failed: {}, status: {}", total, success, failed, status);
        return new BulkImportResponse(status, total, success, failed, errorCsvFile);
    }

    private void processChunkWithPipeline(List<String[]> chunk, ListType listType, String performedBy) {
        log.trace("Opening Redis pipeline for a chunk of {} users", chunk.size());
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            org.springframework.data.redis.connection.StringRedisConnection stringConn =
                (org.springframework.data.redis.connection.StringRedisConnection) connection;

            for (String[] row : chunk) {
                String userId = row[0].trim();
                String targetKey = buildKey(listType, userId);

                Integer duration = null;
                if (row.length > 1 && !row[1].trim().isEmpty()) {
                    try { duration = Integer.parseInt(row[1].trim()); } catch (Exception ignored) {}
                }

                if (duration == null || duration <= 0) {
                    stringConn.set(targetKey, "true");
                } else {
                    stringConn.setEx(targetKey, duration, "true");
                }
            }
            return null;
        });
        log.trace("Pipeline flushed successfully for chunk of {} users", chunk.size());
    }
}
