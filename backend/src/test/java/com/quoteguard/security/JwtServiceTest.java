package com.quoteguard.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

/**
 * Pure unit tests - no Spring context, no database. JwtService has no
 * external dependencies, so these run instantly and everywhere (including CI).
 */
class JwtServiceTest {

    private static final String TEST_SECRET = "unit-test-only-secret-key-must-be-at-least-32-bytes-long!!";

    @Test
    void generateToken_thenExtractUserId_returnsOriginalId() {
        JwtService jwtService = new JwtService(TEST_SECRET, 900_000);

        String token = jwtService.generateToken(42L, "user@example.com");

        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void extractUserId_rejectsTokenSignedWithDifferentSecret() {
        JwtService issuer = new JwtService(TEST_SECRET, 900_000);
        JwtService verifier = new JwtService("a-completely-different-secret-key-32-bytes-min!!", 900_000);

        String token = issuer.generateToken(1L, "a@b.com");

        assertThat(verifier.isValid(token)).isFalse();
        assertThatThrownBy(() -> verifier.extractUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void extractUserId_rejectsExpiredToken() throws InterruptedException {
        JwtService jwtService = new JwtService(TEST_SECRET, 1); // expires 1ms after issuance

        String token = jwtService.generateToken(7L, "expiring@example.com");
        Thread.sleep(20);

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void isValid_rejectsGarbageToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 900_000);

        assertThat(jwtService.isValid("not-a-real-token")).isFalse();
    }
}
