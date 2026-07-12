package com.quoteguard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.quoteguard.dto.AuthResponse;
import com.quoteguard.dto.LoginRequest;
import com.quoteguard.dto.RegisterRequest;
import com.quoteguard.entity.User;
import com.quoteguard.exception.AuthenticationFailedException;
import com.quoteguard.exception.DuplicateResourceException;
import com.quoteguard.repository.UserRepository;
import com.quoteguard.security.JwtService;
import com.quoteguard.security.RefreshTokenService;

/**
 * Pure unit tests (Mockito, no database, no Spring context) for the
 * highest-security-value class in the codebase: registration, login, token
 * refresh, and logout.
 *
 * The two login-failure tests below are the regression guard for the
 * user-enumeration fix - before this feature, "no such user" and "wrong
 * password" produced two different, distinguishable error messages, letting
 * an attacker enumerate registered emails one login attempt at a time. Both
 * paths must now throw AuthenticationFailedException with the exact same
 * message.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService, refreshTokenService);
    }

    @Test
    void register_savesUserWithEncodedPasswordAndDefaultRole() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "correcthorse1");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("correcthorse1")).thenReturn("hashed-value");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getPassword()).isEqualTo("hashed-value"); // never the raw password
        assertThat(saved.getRole()).isEqualTo("USER");
    }

    @Test
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "correcthorse1");
        when(userRepository.findByEmail("jane@example.com"))
                .thenReturn(Optional.of(User.builder().id(1L).email("jane@example.com").build()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_rejectsWeakPassword() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "short");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsTokensForValidCredentials() {
        User user = User.builder().id(1L).email("jane@example.com").password("hashed").role("USER").build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(1L, "jane@example.com")).thenReturn("access-token");
        when(refreshTokenService.issue(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("jane@example.com", "correcthorse1"));

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    void login_unknownEmail_throwsGenericInvalidCredentialsMessage() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "whatever1")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_wrongPassword_throwsTheExactSameMessageAsUnknownEmail() {
        User user = User.builder().id(1L).email("jane@example.com").password("hashed").role("USER").build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad creds"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "wrong-password")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password"); // identical to the unknown-email case above
    }

    @Test
    void refresh_consumesOldTokenAndIssuesRotatedTokens() {
        User user = User.builder().id(1L).email("jane@example.com").build();
        when(refreshTokenService.consume("old-raw-token")).thenReturn(user);
        when(jwtService.generateToken(1L, "jane@example.com")).thenReturn("new-access-token");
        when(refreshTokenService.issue(user)).thenReturn("new-refresh-token");

        AuthResponse response = authService.refresh("old-raw-token");

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenService, times(1)).consume("old-raw-token");
    }

    @Test
    void refresh_rejectsUnknownOrExpiredToken() {
        when(refreshTokenService.consume(anyString())).thenThrow(new java.util.NoSuchElementException());

        assertThatThrownBy(() -> authService.refresh("garbage-token"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void logout_delegatesToRefreshTokenServiceRevoke() {
        authService.logout("some-refresh-token");

        verify(refreshTokenService, times(1)).revoke("some-refresh-token");
    }
}
