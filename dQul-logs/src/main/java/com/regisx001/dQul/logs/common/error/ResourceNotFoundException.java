package com.regisx001.dQul.logs.common.error;

/**
 * Thrown when a requested resource (e.g. a log entry) cannot be found.
 * Mapped to HTTP 404 by the {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
