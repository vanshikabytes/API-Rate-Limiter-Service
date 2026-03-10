package com.vanshika.api_rate_limiter_service.model;

public enum TimeWindow {
    SECOND(1),
    MINUTE(60),
    HOUR(3600);

    private final long seconds;

    TimeWindow(long seconds) {
        this.seconds = seconds;
    }

    public long getSeconds() {
        return seconds;
    }

    public static TimeWindow fromString(String window) {
        try {
            return TimeWindow.valueOf(window.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MINUTE;
        }
    }
}
