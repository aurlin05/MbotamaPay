package com.mbotamapay.backend.service;

public interface EmailService {
    void sendOtpEmail(String to, String code);

    void sendEmail(String to, String subject, String content);
}
