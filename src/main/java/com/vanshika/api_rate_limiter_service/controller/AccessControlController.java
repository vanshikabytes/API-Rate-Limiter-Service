package com.vanshika.api_rate_limiter_service.controller;

import com.vanshika.api_rate_limiter_service.dto.AccessResponse;
import com.vanshika.api_rate_limiter_service.dto.BulkImportResponse;
import com.vanshika.api_rate_limiter_service.dto.PaginatedUsersResponse;
import com.vanshika.api_rate_limiter_service.dto.UpdateAccessReq;
import com.vanshika.api_rate_limiter_service.model.ListType;
import com.vanshika.api_rate_limiter_service.service.AccessControlService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/access/users")
public class AccessControlController {

    private final AccessControlService accessControlService;

    public AccessControlController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    private String getAuthenticatedAdminUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * 1. Add or Update a user on the Blacklist/Whitelist.
     */
    @PatchMapping("/{userId}")
    public ResponseEntity<AccessResponse> setUserAccess(
            @PathVariable String userId,
            @RequestBody UpdateAccessReq req) {
        
        String performedBy = getAuthenticatedAdminUsername();
        
        AccessResponse response = accessControlService.setUserAccess(
                userId, 
                req.getListType(), 
                req.getDurationSeconds(), 
                req.getReason(), 
                performedBy
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 2. Lookup a user's current status (TTL).
     */
    @GetMapping("/{userId}/status")
    public ResponseEntity<AccessResponse> getUserStatus(@PathVariable String userId) {
        AccessResponse response = accessControlService.getUserStatus(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. Manually unblock or remove a user from a list.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeUserAccess(
            @PathVariable String userId,
            @RequestParam ListType listType) {
        
        String performedBy = getAuthenticatedAdminUsername();
        accessControlService.removeUserAccess(userId, listType, performedBy);
        return ResponseEntity.ok().build();
    }

    /**
     * 4. View all users (Paginated).
     */
    @GetMapping
    public ResponseEntity<PaginatedUsersResponse> getAllUsers(
            @RequestParam ListType listType,
            @RequestParam(defaultValue = "0") String cursor,
            @RequestParam(defaultValue = "100") int count) {
        
        PaginatedUsersResponse response = accessControlService.getAllUsers(listType, count);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. Bulk Import via CSV.
     */
    @PostMapping("/bulk")
    public org.springframework.http.ResponseEntity<?> processBulkCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam ListType listType) {
        
        String performedBy = getAuthenticatedAdminUsername();
        BulkImportResponse response = accessControlService.processBulkCsv(file, listType, performedBy);
        
        if (response.getFailed() > 0) {
            return org.springframework.http.ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=failed_records.csv")
                    .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                    .body(response.getErrorCsv());
        }
        
        return org.springframework.http.ResponseEntity.ok(response);
    }
}
