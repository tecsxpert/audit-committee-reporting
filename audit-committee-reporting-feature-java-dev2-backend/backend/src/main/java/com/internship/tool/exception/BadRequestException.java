package com.internship.tool.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the request data is invalid.
 * Spring automatically returns HTTP 400 when this is thrown.
 *
 * Example usage:
 *   throw new BadRequestException("Username already exists");
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}