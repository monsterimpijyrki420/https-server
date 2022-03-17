package com.server;

import java.time.LocalDateTime;

public class UserCoordinate {
    private String username;
    private LocalDateTime timestamp;

    public UserCoordinate(String username, LocalDateTime timestamp) {
        this.username = username;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
