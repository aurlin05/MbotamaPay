package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findByRecipientAndCodeAndVerifiedFalseAndExpiryTimeAfter(
            String recipient, String code, LocalDateTime currentTime);

    void deleteByExpiryTimeBefore(LocalDateTime expiryTime);
}
