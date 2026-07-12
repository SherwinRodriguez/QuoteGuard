package com.quoteguard.service;

import java.util.NoSuchElementException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.quoteguard.dto.AuthResponse;
import com.quoteguard.dto.LoginRequest;
import com.quoteguard.dto.RegisterRequest;
import com.quoteguard.entity.User;
import com.quoteguard.exception.AuthenticationFailedException;
import com.quoteguard.exception.DuplicateResourceException;
import com.quoteguard.repository.UserRepository;
import com.quoteguard.security.JwtService;
import com.quoteguard.security.PasswordPolicy;
import com.quoteguard.security.RefreshTokenService;

import lombok.RequiredArgsConstructor;

/**
 * Every failure case here throws a typed exception handled centrally by
 * GlobalExceptionHandler, instead of the previous approach of returning a
 * 200 OK with a String/Map body that the controller then string-matched to
 * decide the real status code. Login and refresh both throw the SAME
 * AuthenticationFailedException with the SAME message for every failure
 * reason (no such user, wrong password, expired/invalid refresh token) -
 * this closes a user-enumeration weakness that the previous
 * "user not found" vs "Password Mismatch" distinction had, which was
 * flagged and deliberately deferred during the JWT authentication feature.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        // Throws IllegalArgumentException (-> 400 via GlobalExceptionHandler)
        // if the password is too weak.
        PasswordPolicy.validate(request.getPassword());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .build();

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS_MESSAGE));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS_MESSAGE);
        }

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
        String refreshToken = refreshTokenService.issue(user);

        return new AuthResponse(accessToken, refreshToken, "Bearer", user.getId());
    }

    /** Exchanges a valid refresh token for a new access token and a new (rotated) refresh token. */
    public AuthResponse refresh(String rawRefreshToken) {
        User user;
        try {
            user = refreshTokenService.consume(rawRefreshToken);
        } catch (NoSuchElementException e) {
            throw new AuthenticationFailedException("Invalid or expired refresh token");
        }

        String newAccessToken = jwtService.generateToken(user.getId(), user.getEmail());
        String newRefreshToken = refreshTokenService.issue(user);

        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer", user.getId());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }
}
