package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.entity.FeeRule;
import com.mbotamapay.backend.entity.OperatorAccount;
import com.mbotamapay.backend.entity.TransactionType;
import com.mbotamapay.backend.repository.FeeRuleRepository;
import com.mbotamapay.backend.service.FeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeServiceImpl implements FeeService {

    private final FeeRuleRepository feeRuleRepository;

    @Override
    public BigDecimal calculateFee(BigDecimal amount, TransactionType type, OperatorAccount.Provider provider) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        Optional<FeeRule> ruleOpt = feeRuleRepository
                .findByProviderAndTransactionTypeAndActiveTrue(provider != null ? provider.name() : null, type);

        if (ruleOpt.isEmpty()) {
            // Try generic rule (null provider) if specific provider rule not found
            ruleOpt = feeRuleRepository.findByProviderAndTransactionTypeAndActiveTrue(null, type);
        }

        if (ruleOpt.isPresent()) {
            FeeRule rule = ruleOpt.get();
            BigDecimal percentageFee = amount
                    .multiply(rule.getPercentageFee().divide(new BigDecimal(100), 4, RoundingMode.HALF_UP));
            BigDecimal totalFee = percentageFee.add(rule.getFixedFee());

            // Apply min/max limits if they existed in entity (currently they don't, but
            // good practice to consider)

            return totalFee.setScale(0, RoundingMode.CEILING); // Round up to nearest integer for currency units like
                                                               // XAF
        }

        return BigDecimal.ZERO;
    }

    @Override
    public String getFeeDescription(TransactionType type, OperatorAccount.Provider provider) {
        Optional<FeeRule> ruleOpt = feeRuleRepository
                .findByProviderAndTransactionTypeAndActiveTrue(provider != null ? provider.name() : null, type);

        if (ruleOpt.isEmpty()) {
            ruleOpt = feeRuleRepository.findByProviderAndTransactionTypeAndActiveTrue(null, type);
        }

        if (ruleOpt.isPresent()) {
            FeeRule rule = ruleOpt.get();
            return String.format("%s%% + %s", rule.getPercentageFee(), rule.getFixedFee());
        }

        return "Free";
    }
}
