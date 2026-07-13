package com.ups.billing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Persistent representation of a registered user. Mapped to the {@code users}
 * table via JPA/Hibernate, so it works against H2 today and Supabase/Postgres
 * once the datasource is wired in. The {@code passwordHash} is a BCrypt hash and
 * must never be returned to clients.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "access_key", nullable = false)
    private String accessKey;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required no-arg constructor for JPA. */
    protected User() {
    }

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
