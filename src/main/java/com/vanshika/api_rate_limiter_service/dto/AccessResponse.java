package com.vanshika.api_rate_limiter_service.dto;

public class AccessResponse {
    private String status;
    private String userId;
    private String listType;
    private boolean isPermanent;
    private String expiresAt;

    public AccessResponse(String status, String userId, String listType, boolean isPermanent, String expiresAt) {
        this.status = status;
        this.userId = userId;
        this.listType = listType;
        this.isPermanent = isPermanent;
        this.expiresAt = expiresAt;
    }

    // Getters
    public String getStatus() { return status; }
    public String getUserId() { return userId; }
    public String getListType() { return listType; }
    public boolean isPermanent() { return isPermanent; }
    public String getExpiresAt() { return expiresAt; }
}
