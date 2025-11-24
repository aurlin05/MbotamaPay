package com.mbotamapay.backend.utils;

import com.mbotamapay.backend.entity.KycLevel;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.exception.TransactionLimitExceededException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionLimitValidator {

    private static final BigDecimal KYC_LEVEL_1_LIMIT = new BigDecimal("50000");
    private static final BigDecimal KYC_LEVEL_2_LIMIT = new BigDecimal("500000");

    public void validateTransactionLimit(User user, BigDecimal amount) {
        BigDecimal limit = getTransactionLimit(user.getKycLevel());

        if (amount.compareTo(limit) > 0) {
            throw new TransactionLimitExceededException(
                    String.format("Transaction amount %.2f XAF exceeds your KYC %s limit of %.2f XAF",
                            amount, user.getKycLevel(), limit));
        }
    }

    private BigDecimal getTransactionLimit(KycLevel kycLevel) {
        return switch (kycLevel) {
            case LEVEL_1 -> KYC_LEVEL_1_LIMIT;
            case LEVEL_2 -> KYC_LEVEL_2_LIMIT;
            case UNVERIFIED -> BigDecimal.ZERO;
            default -> BigDecimal.ZERO;
        };
    }

    public BigDecimal getLimit(KycLevel kycLevel) {
        return getTransactionLimit(kycLevel);
    }
}
