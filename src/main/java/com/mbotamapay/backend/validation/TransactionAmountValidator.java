package com.mbotamapay.backend.validation;

import com.mbotamapay.backend.entity.KycLevel;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.security.CustomUserDetails;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

public class TransactionAmountValidator implements ConstraintValidator<ValidTransactionAmount, BigDecimal> {

    // KYC limits based on level
    private static final BigDecimal UNVERIFIED_LIMIT = new BigDecimal("100000");   // 100,000 XAF
    private static final BigDecimal LEVEL_1_LIMIT = new BigDecimal("500000");      // 500,000 XAF
    private static final BigDecimal LEVEL_2_LIMIT = new BigDecimal("10000000");    // 10,000,000 XAF

    @Override
    public void initialize(ValidTransactionAmount constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        // Check if amount is positive
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Amount must be positive")
                    .addConstraintViolation();
            return false;
        }

        // Get current user's KYC level from security context
        KycLevel kycLevel = getCurrentUserKycLevel();
        BigDecimal maxLimit = getMaxLimitForKycLevel(kycLevel);

        // Check if amount exceeds KYC limit
        if (value.compareTo(maxLimit) > 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Amount exceeds limit for KYC level %s (max: %s)", kycLevel, maxLimit))
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    private KycLevel getCurrentUserKycLevel() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                User user = userDetails.getUser();
                return user.getKycLevel() != null ? user.getKycLevel() : KycLevel.UNVERIFIED;
            }
        } catch (Exception e) {
            // If we can't get the user, default to UNVERIFIED
        }
        return KycLevel.UNVERIFIED;
    }

    private BigDecimal getMaxLimitForKycLevel(KycLevel kycLevel) {
        if (kycLevel == null) {
            return UNVERIFIED_LIMIT;
        }
        
        return switch (kycLevel) {
            case UNVERIFIED -> UNVERIFIED_LIMIT;
            case LEVEL_1 -> LEVEL_1_LIMIT;
            case LEVEL_2 -> LEVEL_2_LIMIT;
        };
    }
}
