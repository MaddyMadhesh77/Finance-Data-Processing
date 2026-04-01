package com.zorvyn.finance.exception;

/**
 * Thrown when a resource already exists (e.g., duplicate username).
 * Maps to HTTP 409 in the global exception handler.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
