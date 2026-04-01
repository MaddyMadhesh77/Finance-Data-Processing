package com.zorvyn.finance.exception;

/**
 * Thrown when a requested resource does not exist or has been soft-deleted.
 * Maps to HTTP 404 in the global exception handler.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
