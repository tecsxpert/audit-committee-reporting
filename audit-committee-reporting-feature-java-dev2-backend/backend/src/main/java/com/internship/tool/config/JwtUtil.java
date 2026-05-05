package com.internship.tool.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Utility class for creating and validating JWT tokens.
 *
 * A JWT token looks like: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.xxxx
 * It contains: username, role, issued time, expiry time.
 * Anyone with the secret key can verify it hasn't been tampered with.
 *
 * NOTE: Coordinate with Java Developer-1 — they may own this file.
 * Include it in your project if Dev-1 hasn't written it yet.
 */
@Component
public class JwtUtil {

    // Secret key from application.yml → from .env file
    @Value("${jwt.secret}")
    private String secret;

    // Token validity duration (24 hours = 86400000 ms)
    @Value("${jwt.expiration:86400000}")
    private long expiration;

    /**
     * Creates a signing key from the secret string.
     * HMAC-SHA256 algorithm is used.
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generates a JWT token for a logged-in user.
     * Embeds username and role inside the token.
     *
     * @param username  the user's login name
     * @param role      the user's role (ADMIN, MANAGER, VIEWER)
     * @return          a signed JWT string
     */
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)                              // who this token belongs to
                .claim("role", role)                               // embed role in token
                .setIssuedAt(new Date())                           // when was it created
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // when it expires
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the username (subject) from a token.
     * Used in JwtAuthFilter to identify who is making the request.
     */
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extracts the role from a token.
     */
    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
    }

    /**
     * Checks if a token is still valid (not expired, not tampered).
     * Returns false if token is invalid — never throws an exception to the caller.
     */
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);   // throws if invalid or expired
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parses the token and returns all claims (the data stored inside).
     * Throws JwtException if the token is expired or tampered with.
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}