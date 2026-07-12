package com.quoteguard.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.quoteguard.entity.RefreshToken;
import com.quoteguard.entity.User;
import com.quoteguard.repository.RefreshTokenRepository;

/**
 * Issues, validates, and rotates opaque refresh tokens.
 *
 * Refresh tokens are deliberately NOT JWTs: unlike access tokens, they must
 * be revocable on demand (logout), which requires server-side state that a
 * self-contained signed token can't provide without an additional
 * revocation-list lookup anyway - so there is no benefit to making them
 * JWTs here. Only a SHA-256 hash of the token is persisted (never the raw
 * value), mirroring password storage: a database leak alone should not
 * hand out directly usable refresh tokens.
 *
 * Rotation: every successful refresh deletes the presented token and
 * issues a brand new one. If an already-used (rotated-away) token is
 * presented again, validation fails - a signal of possible token theft.
 * Reacting to that signal by revoking every token for the user is a
 * natural next step, intentionally not built here to avoid adding
 * mechanism ahead of a demonstrated need.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String issue(User user) {
        String rawToken = generateOpaqueToken();

        RefreshToken entity = RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates the presented refresh token and deletes it (rotation),
     * returning the owning user so the caller can issue new tokens.
     *
     * @throws NoSuchElementException if the token is unknown, already used, or expired
     */
    public User consume(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new NoSuchElementException("Invalid or already-used refresh token"));

        // Deleted unconditionally, even if expired below: an expired token
        // must never be presentable twice either.
        refreshTokenRepository.delete(stored);

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new NoSuchElementException("Refresh token has expired");
        }

        return stored.getUser();
    }

    /** Idempotent: succeeds whether or not the token still exists. */
    public void revoke(String rawToken) {
        refreshTokenRepository.deleteByTokenHash(hash(rawToken));
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
