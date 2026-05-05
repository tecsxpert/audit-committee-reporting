package com.internship.tool.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration.
 * Defines which URLs are public and which require a valid JWT token.
 *
 * @EnableMethodSecurity enables @PreAuthorize on controller methods.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * The main security rule chain.
     * Order matters — rules are checked top to bottom.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — we use JWT, not cookies
                .csrf(csrf -> csrf.disable())

                // No sessions — every request must carry a JWT token
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Define which endpoints are public vs protected
                .authorizeHttpRequests(auth -> auth

                        // ── PUBLIC endpoints (no token needed) ──────────────────
                        .requestMatchers("/auth/**").permitAll()             // login and register
                        .requestMatchers("/actuator/health").permitAll()     // Docker healthcheck
                        .requestMatchers("/swagger-ui/**").permitAll()       // API docs
                        .requestMatchers("/v3/api-docs/**").permitAll()      // OpenAPI spec
                        .requestMatchers("/swagger-ui.html").permitAll()

                        // ── PROTECTED endpoints (valid JWT required) ─────────────
                        .anyRequest().authenticated()
                )

                // Add JWT filter before the default username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder — hashes passwords before storing.
     * Strength 12 = secure but not too slow.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}