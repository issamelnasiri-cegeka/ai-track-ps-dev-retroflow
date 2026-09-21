package com.stackcraft.retroflow.exception;

/**
 * Thrown when a requested resource (team, retrospective, feedback item,
 * action item, ...) cannot be found by its identifier. Mapped to
 * HTTP 404 Not Found by {@link com.stackcraft.retroflow.web.GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

}
