package com.vanshika.api_rate_limiter_service.dto;

import com.vanshika.api_rate_limiter_service.model.ListType;

public class UpdateAccessReq {
    private ListType listType;
    private Integer durationSeconds;
    private String reason;

    // Getters and Setters
    public ListType getListType() { return listType; }
    public void setListType(ListType listType) { this.listType = listType; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
