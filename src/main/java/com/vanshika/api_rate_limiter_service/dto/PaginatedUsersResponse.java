package com.vanshika.api_rate_limiter_service.dto;

import java.util.List;

public class PaginatedUsersResponse {
    private String listType;
    private String nextCursor;
    private List<UserStatus> users;

    public PaginatedUsersResponse(String listType, String nextCursor, List<UserStatus> users) {
        this.listType = listType;
        this.nextCursor = nextCursor;
        this.users = users;
    }

    public static class UserStatus {
        private String userId;
        private Long remainingSeconds;

        public UserStatus(String userId, Long remainingSeconds) {
            this.userId = userId;
            this.remainingSeconds = remainingSeconds;
        }
        public String getUserId() { return userId; }
        public Long getRemainingSeconds() { return remainingSeconds; }
    }

    // Getters
    public String getListType() { return listType; }
    public String getNextCursor() { return nextCursor; }
    public List<UserStatus> getUsers() { return users; }
}
