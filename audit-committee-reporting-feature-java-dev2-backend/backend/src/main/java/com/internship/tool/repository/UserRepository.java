package com.internship.tool.repository;

import com.internship.tool.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Database queries for the app_user table.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their username.
     * Used during login to check if the user exists.
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks if a username is already taken.
     * Used during registration to prevent duplicates.
     */
    boolean existsByUsername(String username);

    /**
     * Checks if an email is already registered.
     * Used during registration to prevent duplicates.
     */
    boolean existsByEmail(String email);
}