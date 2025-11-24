package com.mbotamapay.backend.service;

import com.mbotamapay.backend.entity.TransactionType;
import com.mbotamapay.backend.entity.OperatorAccount;

import java.math.BigDecimal;

public interface FeeService {

    /**
     * Calculate fee for a transaction
     * 
     * @param amount   Transaction amount
     * @param type     Transaction type
     * @param provider Provider (optional, for external transactions)
     * @return Calculated fee amount
     */
    BigDecimal calculateFee(BigDecimal amount, TransactionType type, OperatorAccount.Provider provider);

    /**
     * Get fee description (e.g., "1.5% + 100 XAF")
     */
    String getFeeDescription(TransactionType type, OperatorAccount.Provider provider);
}
