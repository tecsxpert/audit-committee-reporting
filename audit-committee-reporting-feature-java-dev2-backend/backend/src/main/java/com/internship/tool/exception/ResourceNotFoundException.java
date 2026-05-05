package com.internship.tool.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested record does not exist in the database.
 * Spring automatically returns HTTP 404 when this is thrown.
 *
 * Example usage:
 *   throw new ResourceNotFoundException("Report not found with id: " + id);
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}