package com.mbotamapay.backend.service;

import com.mbotamapay.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Run daily at 3 AM to clean up expired refresh tokens
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting expired refresh token cleanup");
        try {
            refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("Expired refresh token cleanup completed");
        } catch (Exception e) {
            log.error("Error cleaning up expired refresh tokens", e);
        }
    }
}
