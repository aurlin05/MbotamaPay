package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.liquidity.RebalanceAction;
import com.mbotamapay.backend.dto.liquidity.RebalanceSuggestion;
import com.mbotamapay.backend.entity.OperatorAccount;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.repository.OperatorAccountRepository;
import com.mbotamapay.backend.service.LiquidityManager;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiquidityManagerImpl implements LiquidityManager {

    private final OperatorAccountRepository operatorAccountRepository;

    // In-memory reservation store (Note: In production, use Redis or DB)
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();

    @Data
    private static class Reservation {
        private String id;
        private OperatorAccount.Provider provider;
        private String currency;
        private BigDecimal amount;
        private LocalDateTime createdAt;
    }

    @Override
    @Transactional
    public String reserveForPayout(OperatorAccount.Provider provider, String currency, BigDecimal amount) {
        log.info("Reserving liquidity: {} {} from {}", amount, currency, provider);

        OperatorAccount account = operatorAccountRepository.findByProviderAndCurrencyWithLock(provider, currency)
                .orElseThrow(
                        () -> new BusinessException("Operator account not found for " + provider + " " + currency));

        if (!account.hasSufficientBalance(amount)) {
            throw new BusinessException("Insufficient liquidity for " + provider + " " + currency);
        }

        // Update balances
        account.setBalance(account.getBalance().subtract(amount));
        account.setReservedBalance(account.getReservedBalance().add(amount));
        operatorAccountRepository.save(account);

        // Create reservation record
        String reservationId = UUID.randomUUID().toString();
        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setProvider(provider);
        reservation.setCurrency(currency);
        reservation.setAmount(amount);
        reservation.setCreatedAt(LocalDateTime.now());

        reservations.put(reservationId, reservation);

        return reservationId;
    }

    @Override
    @Transactional
    public void releaseReservation(String reservationId) {
        log.info("Releasing reservation: {}", reservationId);

        Reservation reservation = reservations.remove(reservationId);
        if (reservation == null) {
            log.warn("Reservation not found or already processed: {}", reservationId);
            return;
        }

        OperatorAccount account = operatorAccountRepository
                .findByProviderAndCurrencyWithLock(reservation.getProvider(), reservation.getCurrency())
                .orElseThrow(() -> new BusinessException("Operator account not found"));

        // Rollback balances
        account.setBalance(account.getBalance().add(reservation.getAmount()));
        account.setReservedBalance(account.getReservedBalance().subtract(reservation.getAmount()));
        operatorAccountRepository.save(account);
    }

    @Override
    @Transactional
    public void confirmReservation(String reservationId) {
        log.info("Confirming reservation: {}", reservationId);

        Reservation reservation = reservations.remove(reservationId);
        if (reservation == null) {
            log.warn("Reservation not found or already processed: {}", reservationId);
            return;
        }

        OperatorAccount account = operatorAccountRepository
                .findByProviderAndCurrencyWithLock(reservation.getProvider(), reservation.getCurrency())
                .orElseThrow(() -> new BusinessException("Operator account not found"));

        // Confirm usage (remove from reserved)
        account.setReservedBalance(account.getReservedBalance().subtract(reservation.getAmount()));
        operatorAccountRepository.save(account);
    }

    @Override
    public BigDecimal getAvailableBalance(OperatorAccount.Provider provider, String currency) {
        return operatorAccountRepository.findByProviderAndCurrency(provider, currency)
                .map(OperatorAccount::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public RebalanceSuggestion suggestRebalancing() {
        List<RebalanceAction> actions = new ArrayList<>();
        List<OperatorAccount> accounts = operatorAccountRepository.findAll();

        // Simple threshold-based logic
        BigDecimal lowThreshold = new BigDecimal("100000"); // 100k units
        BigDecimal highThreshold = new BigDecimal("1000000"); // 1M units

        for (OperatorAccount account : accounts) {
            if (account.getBalance().compareTo(lowThreshold) < 0) {
                // This account is low, find a donor
                findDonor(account, accounts, highThreshold).ifPresent(donor -> {
                    actions.add(RebalanceAction.builder()
                            .fromProvider(donor.getProvider())
                            .toProvider(account.getProvider())
                            .currency(account.getCurrency())
                            .amount(new BigDecimal("500000")) // Suggest moving 500k
                            .reason("Low balance on " + account.getProvider())
                            .build());
                });
            }
        }

        return RebalanceSuggestion.builder()
                .actions(actions)
                .analysisTimestamp(LocalDateTime.now().toString())
                .build();
    }

    private java.util.Optional<OperatorAccount> findDonor(OperatorAccount target, List<OperatorAccount> accounts,
            BigDecimal threshold) {
        return accounts.stream()
                .filter(a -> a.getCurrency().equals(target.getCurrency()))
                .filter(a -> !a.getProvider().equals(target.getProvider()))
                .filter(a -> a.getBalance().compareTo(threshold) > 0)
                .findFirst();
    }
}
