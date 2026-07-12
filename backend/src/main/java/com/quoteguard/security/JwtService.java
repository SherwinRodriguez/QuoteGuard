package com.quoteguard.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and validates HS256-signed JWT access tokens.
 *
 * The token subject is the numeric User ID (a stable, immutable identifier),
 * not the email (which a user could change later). Do NOT put anything
 * sensitive (password, role, etc.) in the claims - JWTs are signed, not
 * encrypted, and are fully readable by anyone holding the token.
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:900000}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and validates the token's signature and expiry.
     *
     * @return the User ID stored in the subject claim
     * @throws JwtException            if the token is malformed, expired, or has an invalid signature
     * @throws IllegalArgumentException if the token is null/blank
     */
    public Long extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    public boolean isValid(String token) {
        try {
            extractUserId(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException already covers ExpiredJwtException (it's a
            // subclass) - a multi-catch listing both is rejected by javac
            // as redundant/ambiguous ("alternatives related by subclassing").
            // Behavior is unchanged: any parse failure, including expiry,
            // is treated as "not valid".
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
