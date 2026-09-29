package com.vanshika.api_rate_limiter_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class BulkImportResponse {
    private String status;
    private int totalProcessed;
    private int successful;
    private int failed;
    
    @JsonIgnore
    private byte[] errorCsv;

    public BulkImportResponse(String status, int totalProcessed, int successful, int failed, byte[] errorCsv) {
        this.status = status;
        this.totalProcessed = totalProcessed;
        this.successful = successful;
        this.failed = failed;
        this.errorCsv = errorCsv;
    }

    public String getStatus() { return status; }
    public int getTotalProcessed() { return totalProcessed; }
    public int getSuccessful() { return successful; }
    public int getFailed() { return failed; }
    public byte[] getErrorCsv() { return errorCsv; }
}
