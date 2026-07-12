package com.quoteguard.exception;

/** Thrown when a create operation collides with an existing unique resource (email, invoice number). Mapped to 409 by GlobalExceptionHandler. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
