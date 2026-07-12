package com.quoteguard.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central exception-to-HTTP translation, returning RFC 9457 Problem Details
 * (application/problem+json) for every error case. Previously this class
 * was an empty 3-line stub with no annotations - every unhandled exception
 * fell through to Spring Boot's default whitelabel error handling, and
 * every "expected" failure (not found, duplicate, etc.) was hand-mapped to
 * a status code individually inside each controller.
 *
 * IMPORTANT NUANCE on AccessDeniedException: the handler below intercepts
 * BEFORE Spring Security's own ExceptionTranslationFilter gets a chance,
 * for any AccessDeniedException thrown during controller/service execution
 * (e.g. ClientService/InvoiceService's ownership checks). Spring MVC's own
 * exception-resolver chain - which this @RestControllerAdvice participates
 * in - runs inside DispatcherServlet, which itself sits INSIDE the security
 * filter chain; once this handler resolves the exception, it never
 * propagates out to the filter chain at all. The 401 case (missing/invalid
 * bearer token) is a different code path entirely: it's decided by
 * authorizeHttpRequests() before the request ever reaches a controller, so
 * it is still produced by the custom AuthenticationEntryPoint configured in
 * SecurityConfig, unaffected by this class.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        return problemDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ProblemDetail handleAuthenticationFailed(AuthenticationFailedException ex) {
        return problemDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problemDetail(HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflict(IllegalStateException ex) {
        return problemDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return problemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Catches the genuine race-condition case that the new unique
     * constraint on (user_id, invoice_number) exists to guard: two
     * concurrent requests both pass the application-level existsBy check,
     * then one loses at the database level. Without this handler that
     * would surface as a raw 500 with a leaked SQL exception message.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return problemDetail(HttpStatus.CONFLICT,
                "This operation conflicts with existing data (e.g. a duplicate invoice number created concurrently)");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = problemDetail(HttpStatus.BAD_REQUEST, "Request validation failed");

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        problem.setProperty("errors", fieldErrors);

        return problem;
    }

    /**
     * Last line of defense: prevents any unexpected exception from leaking
     * a stack trace or internal detail to the client. The real exception is
     * still logged server-side at ERROR for diagnosis.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
    }

    private ProblemDetail problemDetail(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
