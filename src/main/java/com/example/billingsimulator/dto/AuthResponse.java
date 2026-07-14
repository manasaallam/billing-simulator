package com.example.billingsimulator.dto;

import com.example.billingsimulator.model.AppUser;

/**
 * Success payload returned after signup or login. Contains the JWT token.
 * Never contains the password hash.
 */
public class AuthResponse {

    private final String token;
    private final String userId;
    private final String companyId;
    private final String name;
    private final String email;
    private final String role;

    public AuthResponse(String token, String userId, String companyId, String name, String email, String role) {
        this.token = token;
        this.userId = userId;
        this.companyId = companyId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public static AuthResponse of(AppUser user, String token) {
        return new AuthResponse(
                token,
                user.getUserId().toString(),
                user.getCompanyId().toString(),
                user.getFullName(),
                user.getEmail(),
                user.getRole());
    }

    public String getToken()     { return token; }
    public String getUserId()    { return userId; }
    public String getCompanyId() { return companyId; }
    public String getName()      { return name; }
    public String getEmail()     { return email; }
    public String getRole()      { return role; }
}
