package com.mbotamapay.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import com.mbotamapay.backend.service.OtpService;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasksConfig {

    private final OtpService otpService;

    /**
     * Clean up expired OTP codes every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredOtps() {
        otpService.cleanupExpiredOtps();
    }
}
