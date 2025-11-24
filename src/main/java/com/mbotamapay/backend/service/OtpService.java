package com.mbotamapay.backend.service;

import com.mbotamapay.backend.entity.OtpType;

public interface OtpService {
    void generateAndSendOtp(String recipient, OtpType type);

    boolean verifyOtp(String recipient, String code);

    void cleanupExpiredOtps();
}
