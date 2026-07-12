package com.quoteguard.security;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.quoteguard.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * Purges expired refresh tokens hourly so the table doesn't grow
 * unbounded. Pure housekeeping - no application behavior depends on this
 * running promptly: an expired token is already rejected by
 * RefreshTokenService.consume() regardless of whether cleanup has run.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupTask.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void purgeExpiredTokens() {
        int deleted = refreshTokenRepository.deleteAllExpiredBefore(Instant.now());
        if (deleted > 0) {
            log.info("Purged {} expired refresh token(s)", deleted);
        }
    }
}
