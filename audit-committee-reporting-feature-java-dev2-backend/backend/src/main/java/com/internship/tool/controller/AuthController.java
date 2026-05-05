package com.internship.tool.controller;

import com.internship.tool.dto.LoginRequest;
import com.internship.tool.dto.LoginResponse;
import com.internship.tool.dto.RegisterRequest;
import com.internship.tool.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Handles user login, registration, and token refresh.
 * These endpoints are PUBLIC — no JWT token required.
 * Base URL: http://localhost:8080/auth
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Login, register, and token refresh")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /auth/login
     * Body: { "username": "admin", "password": "admin123" }
     * Returns: { "token": "eyJ...", "role": "ADMIN" }
     */
    @Operation(summary = "Login and receive JWT token")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /auth/register
     * Body: { "username": "newuser", "password": "pass123", "email": "u@email.com" }
     * New users always get the VIEWER role by default.
     * Returns 201 Created.
     */
    @Operation(summary = "Register a new user (gets VIEWER role)")
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(201).body("User registered successfully");
    }

    /**
     * POST /auth/refresh
     * Body: { "token": "eyJ..." }
     * Returns a new token with a fresh expiry time.
     */
    @Operation(summary = "Refresh an expiring JWT token")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody String oldToken) {
        return ResponseEntity.ok(authService.refresh(oldToken));
    }
}