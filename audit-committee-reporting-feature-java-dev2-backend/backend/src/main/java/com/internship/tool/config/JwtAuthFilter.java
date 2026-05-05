package com.internship.tool.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * This filter runs on EVERY incoming HTTP request (once per request).
 * It reads the Authorization header, validates the JWT, and
 * tells Spring Security who is making the request.
 *
 * Flow:
 * 1. Request arrives → this filter runs
 * 2. Read "Authorization: Bearer eyJ..." header
 * 3. Validate the token using JwtUtil
 * 4. Extract username and role from token
 * 5. Set the user in Spring's SecurityContext
 * 6. Pass request to the controller
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        // 1. Get the Authorization header
        String authHeader = request.getHeader("Authorization");

        // 2. If no token or wrong format, skip to next filter (request stays anonymous)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the token (remove "Bearer " prefix)
        String token = authHeader.substring(7);

        // 4. Validate the token
        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Extract username and role from the token
        String username = jwtUtil.extractUsername(token);
        String role     = jwtUtil.extractRole(token);

        // 6. Tell Spring Security: "this request is from [username] with [role]"
        //    ROLE_ prefix is required by Spring Security for @PreAuthorize("hasRole('ADMIN')")
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 7. Continue to the controller
        filterChain.doFilter(request, response);
    }
}