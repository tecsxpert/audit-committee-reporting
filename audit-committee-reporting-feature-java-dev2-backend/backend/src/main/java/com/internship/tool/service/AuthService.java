package com.internship.tool.service;

import com.internship.tool.config.JwtUtil;
import com.internship.tool.dto.LoginRequest;
import com.internship.tool.dto.LoginResponse;
import com.internship.tool.dto.RegisterRequest;
import com.internship.tool.entity.User;
import com.internship.tool.exception.BadRequestException;
import com.internship.tool.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles user authentication: login, register, and token refresh.
 * Uses BCrypt to safely hash passwords — never stored as plain text.
 */
@Service
public class AuthService {

    private final UserRepository  userRepo;
    private final PasswordEncoder passwordEncoder;   // BCrypt hasher
    private final JwtUtil         jwtUtil;

    public AuthService(UserRepository userRepo,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepo        = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil         = jwtUtil;
    }

    /**
     * Validates username + password, then returns a JWT token.
     * Throws BadCredentialsException (401) if login fails.
     */
    public LoginResponse login(LoginRequest request) {

        // 1. Find the user by username
        User user = userRepo.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        // 2. Check if account is active
        if (!user.getIsActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        // 3. Compare the entered password with the stored BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // 4. Generate JWT token with username and role embedded
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        return new LoginResponse(token, user.getUsername(), user.getRole(), 86400000L);
    }

    /**
     * Creates a new user account with VIEWER role.
     * Throws BadRequestException (400) if username or email already exists.
     */
    public void register(RegisterRequest request) {

        // Check for duplicate username
        if (userRepo.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }

        // Check for duplicate email
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        // Create new user — always VIEWER role, password hashed with BCrypt
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(request.getPassword())); // BCrypt hash
        newUser.setEmail(request.getEmail());
        newUser.setRole("VIEWER");   // new users always start as VIEWER

        userRepo.save(newUser);
    }

    /**
     * Takes an existing token and returns a new one with a fresh expiry.
     * Used when the token is about to expire.
     */
    public LoginResponse refresh(String oldToken) {
        // Remove "Bearer " prefix if present
        String cleanToken = oldToken.replace("Bearer ", "").trim();

        // Extract username from the old token
        String username = jwtUtil.extractUsername(cleanToken);

        // Look up the user to get their current role
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Generate a brand new token
        String newToken = jwtUtil.generateToken(user.getUsername(), user.getRole());

        return new LoginResponse(newToken, user.getUsername(), user.getRole(), 86400000L);
    }
}