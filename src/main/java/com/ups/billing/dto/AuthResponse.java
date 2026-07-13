package com.ups.billing.dto;

import com.ups.billing.model.User;

/**
 * Success payload returned to the frontend after signup or login. Never contains
 * the password hash.
 */
public class AuthResponse {

    private final String userId;
    private final String accountId;
    private final String name;
    private final String email;
    private final String role;

    public AuthResponse(String userId, String accountId, String name, String email, String role) {
        this.userId = userId;
        this.accountId = accountId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public static AuthResponse from(User user) {
        return new AuthResponse(
                user.getUserId(),
                user.getAccountId(),
                user.getName(),
                user.getEmail(),
                user.getRole());
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

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
