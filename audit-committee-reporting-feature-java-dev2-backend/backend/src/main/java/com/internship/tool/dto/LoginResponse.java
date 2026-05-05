package com.internship.tool.dto;

/**
 * What the backend sends back after a successful login.
 * Example: { "token": "eyJ...", "username": "admin", "role": "ADMIN" }
 * The frontend stores this token and sends it with every future request.
 */
public class LoginResponse {

    private String token;
    private String username;
    private String role;
    private long   expiresIn;    // milliseconds until token expires

    // ─── Constructor ──────────────────────────────────────────────────────

    public LoginResponse(String token, String username, String role, long expiresIn) {
        this.token     = token;
        this.username  = username;
        this.role      = role;
        this.expiresIn = expiresIn;
    }

    // ─── Getters ──────────────────────────────────────────────────────────

    public String getToken()     { return token; }
    public String getUsername()  { return username; }
    public String getRole()      { return role; }
    public long   getExpiresIn() { return expiresIn; }
}