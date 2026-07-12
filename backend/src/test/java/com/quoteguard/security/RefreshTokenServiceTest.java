package com.quoteguard.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.quoteguard.entity.RefreshToken;
import com.quoteguard.entity.User;
import com.quoteguard.repository.RefreshTokenRepository;

/**
 * Pure unit tests (Mockito, no database) for token issuance, rotation, and
 * revocation.
 */
class RefreshTokenServiceTest {

    private RefreshTokenRepository repository;
    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);
        service = new RefreshTokenService(repository, 604_800_000L); // 7 days
        user = User.builder().id(1L).email("user@example.com").build();
    }

    @Test
    void issue_savesAHashedTokenAndReturnsTheRawValue() {
        String rawToken = service.issue(user);

        assertThat(rawToken).isNotBlank();
        verify(repository, times(1)).save(any(RefreshToken.class));
        // the raw token itself must never be persisted directly - only its hash
    }

    @Test
    void consume_returnsOwnerAndDeletesTheStoredToken_forAValidToken() {
        RefreshToken stored = RefreshToken.builder()
                .tokenHash("irrelevant-in-this-test")
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(repository.findByTokenHash(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(stored));

        User resolved = service.consume("some-raw-token");

        assertThat(resolved).isEqualTo(user);
        verify(repository, times(1)).delete(stored);
    }

    @Test
    void consume_rejectsUnknownToken() {
        when(repository.findByTokenHash(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consume("never-issued"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void consume_rejectsAndDeletesAnExpiredToken() {
        RefreshToken expired = RefreshToken.builder()
                .tokenHash("irrelevant")
                .user(user)
                .expiresAt(Instant.now().minusSeconds(10)) // already expired
                .build();
        when(repository.findByTokenHash(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.consume("expired-token"))
                .isInstanceOf(NoSuchElementException.class);

        // an expired token must still be deleted, not left presentable again
        verify(repository, times(1)).delete(expired);
    }

    @Test
    void revoke_isIdempotentAndDelegatesToDeleteByHash() {
        service.revoke("some-token");

        verify(repository, times(1)).deleteByTokenHash(org.mockito.ArgumentMatchers.anyString());
    }
}
