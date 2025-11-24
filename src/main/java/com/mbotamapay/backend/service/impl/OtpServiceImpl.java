package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.entity.OtpType;
import com.mbotamapay.backend.entity.OtpVerification;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.repository.OtpVerificationRepository;
import com.mbotamapay.backend.service.EmailService;
import com.mbotamapay.backend.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    @Override
    @Transactional
    public void generateAndSendOtp(String recipient, OtpType type) {
        String code = generateOtpCode();

        OtpVerification otp = OtpVerification.builder()
                .recipient(recipient)
                .code(code)
                .type(type)
                .expiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .attempts(0)
                .verified(false)
                .build();

        otpRepository.save(otp);

        // Send OTP based on type
        if (type == OtpType.EMAIL) {
            emailService.sendOtpEmail(recipient, code);
        } else {
            // TODO: Implement SMS service
            log.warn("SMS OTP not yet implemented for: {}", recipient);
        }

        log.info("OTP generated for: {} (type: {})", recipient, type);
    }

    @Override
    @Transactional
    public boolean verifyOtp(String recipient, String code) {
        OtpVerification otp = otpRepository
                .findByRecipientAndCodeAndVerifiedFalseAndExpiryTimeAfter(recipient, code, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("Invalid or expired OTP"));

        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            throw new BusinessException("Maximum OTP attempts exceeded");
        }

        otp.setAttempts(otp.getAttempts() + 1);

        if (otp.getCode().equals(code)) {
            otp.setVerified(true);
            otpRepository.save(otp);
            log.info("OTP verified successfully for: {}", recipient);
            return true;
        }

        otpRepository.save(otp);
        throw new BusinessException("Invalid OTP code");
    }

    @Override
    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
    }

    private String generateOtpCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
