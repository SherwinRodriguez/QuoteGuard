package com.quoteguard.exception;

/**
 * Thrown for any login/refresh credential failure. Deliberately used for
 * BOTH "no such user" and "wrong password" with the same message, so the
 * response can't be used to enumerate registered email addresses. Mapped
 * to 401 by GlobalExceptionHandler.
 */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
