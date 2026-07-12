package com.quoteguard.exception;

/** Thrown when a requested resource (client, invoice, user) does not exist. Mapped to 404 by GlobalExceptionHandler. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
