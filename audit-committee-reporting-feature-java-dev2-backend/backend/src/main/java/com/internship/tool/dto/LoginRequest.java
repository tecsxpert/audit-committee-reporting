package com.internship.tool.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The body the frontend sends to POST /auth/login
 * Example: { "username": "admin", "password": "admin123" }
 */
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    // ─── Getters and Setters ───────────────────────────────────────────────

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}