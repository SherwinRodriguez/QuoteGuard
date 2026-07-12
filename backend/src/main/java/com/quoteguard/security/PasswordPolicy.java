package com.quoteguard.security;

import java.util.regex.Pattern;

/**
 * Minimal, OWASP-reasonable password strength policy, enforced at
 * registration time.
 *
 * Deliberately not gold-plated: beyond a length floor and "at least one
 * letter and one digit," no character-class quotas (uppercase/symbol
 * requirements) are enforced. OWASP's ASVS guidance favors length over
 * forced complexity - composition rules are known to push users toward
 * predictable substitutions (e.g. "Password1!") rather than genuinely
 * stronger passwords.
 *
 * This is a targeted business rule, not a generic field constraint, so it
 * lives here rather than as a Bean Validation @Pattern annotation - full
 * request-level validation for the rest of the DTOs is Phase B.
 */
public final class PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final Pattern HAS_LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");

    private PasswordPolicy() {
    }

    /**
     * @throws IllegalArgumentException with a user-facing message if the password is too weak
     */
    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (!HAS_LETTER.matcher(password).find() || !HAS_DIGIT.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one letter and one digit");
        }
    }
}
