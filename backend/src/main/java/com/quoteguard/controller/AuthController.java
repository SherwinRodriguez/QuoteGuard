package com.quoteguard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quoteguard.dto.AuthResponse;
import com.quoteguard.dto.LoginRequest;
import com.quoteguard.dto.RefreshTokenRequest;
import com.quoteguard.dto.RegisterRequest;
import com.quoteguard.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * All failure cases (duplicate email, weak password, bad credentials,
 * invalid refresh token, validation errors) are handled centrally by
 * GlobalExceptionHandler - there is no manual status-code logic here.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Exchanges a valid, unexpired refresh token for a new access token AND
     * a new refresh token (rotation - see RefreshTokenService). Public:
     * deliberately does not require a valid access token, since the whole
     * point of this endpoint is to recover from an expired one.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    /**
     * Revokes the given refresh token server-side. Idempotent - always
     * succeeds. Public (no bearer access token required) so a user can log
     * out even with an expired access token still sitting in the browser.
     *
     * NOTE: this cannot invalidate an already-issued, still-valid access
     * token - that remains usable until its own short (15 min) expiry.
     * Standard, disclosed trade-off of stateless access tokens.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
