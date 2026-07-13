package com.ups.billing.model;

import java.time.Instant;

/**
 * In-memory representation of a registered user. The {@code passwordHash} is a
 * BCrypt hash and must never be returned to clients.
 */
public class User {

    private final String userId;
    private final String accountId;
    private final String name;
    private final String accessKey;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final Instant createdAt;

    public User(String userId,
                String accountId,
                String name,
                String accessKey,
                String email,
                String passwordHash,
                String role,
                Instant createdAt) {
        this.userId = userId;
        this.accountId = accountId;
        this.name = name;
        this.accessKey = accessKey;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
