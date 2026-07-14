package com.example.billingsimulator.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Validated request body for {@code POST /api/auth/signup}. */
public class SignupRequest {

    @NotBlank(message = "Name is required.")
    private String name;

    @NotBlank(message = "Access key is required.")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Use letters and numbers only (e.g. DEMO2026).")
    private String accessKey;

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password must be at least 8 characters.")
    private String password;

    @NotBlank(message = "Please retype your password.")
    private String confirmPassword;

    public String getName()                        { return name; }
    public void setName(String name)               { this.name = name; }
    public String getAccessKey()                   { return accessKey; }
    public void setAccessKey(String accessKey)     { this.accessKey = accessKey; }
    public String getEmail()                       { return email; }
    public void setEmail(String email)             { this.email = email; }
    public String getPassword()                    { return password; }
    public void setPassword(String password)       { this.password = password; }
    public String getConfirmPassword()             { return confirmPassword; }
    public void setConfirmPassword(String cp)      { this.confirmPassword = cp; }
}
