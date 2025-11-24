package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.liquidity.RebalanceSuggestion;
import com.mbotamapay.backend.entity.OperatorAccount;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.repository.OperatorAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiquidityManagerImplTest {

    @Mock
    private OperatorAccountRepository operatorAccountRepository;

    @InjectMocks
    private LiquidityManagerImpl liquidityManager;

    private OperatorAccount feexpayAccount;

    @BeforeEach
    void setUp() {
        feexpayAccount = OperatorAccount.builder()
                .id(1L)
                .provider(OperatorAccount.Provider.FEEXPAY)
                .currency("XAF")
                .balance(new BigDecimal("500000"))
                .reservedBalance(BigDecimal.ZERO)
                .build();
    }

    @Test
    void testReserveForPayout_Success() {
        // Given
        BigDecimal amount = new BigDecimal("100000");
        when(operatorAccountRepository.findByProviderAndCurrencyWithLock(
                OperatorAccount.Provider.FEEXPAY, "XAF"))
                .thenReturn(Optional.of(feexpayAccount));

        // When
        String reservationId = liquidityManager.reserveForPayout(
                OperatorAccount.Provider.FEEXPAY, "XAF", amount);

        // Then
        assertNotNull(reservationId);
        verify(operatorAccountRepository).save(any(OperatorAccount.class));

        // Verify balance changes
        assertEquals(new BigDecimal("400000"), feexpayAccount.getBalance());
        assertEquals(amount, feexpayAccount.getReservedBalance());
    }

    @Test
    void testReserveForPayout_InsufficientBalance() {
        // Given
        BigDecimal amount = new BigDecimal("600000"); // More than available
        when(operatorAccountRepository.findByProviderAndCurrencyWithLock(
                OperatorAccount.Provider.FEEXPAY, "XAF"))
                .thenReturn(Optional.of(feexpayAccount));

        // When & Then
        assertThrows(BusinessException.class,
                () -> liquidityManager.reserveForPayout(OperatorAccount.Provider.FEEXPAY, "XAF", amount));
    }

    @Test
    void testReleaseReservation_Success() {
        // Given
        BigDecimal amount = new BigDecimal("100000");
        when(operatorAccountRepository.findByProviderAndCurrencyWithLock(
                OperatorAccount.Provider.FEEXPAY, "XAF"))
                .thenReturn(Optional.of(feexpayAccount));

        String reservationId = liquidityManager.reserveForPayout(
                OperatorAccount.Provider.FEEXPAY, "XAF", amount);

        // When
        liquidityManager.releaseReservation(reservationId);

        // Then
        verify(operatorAccountRepository, times(2)).save(any(OperatorAccount.class));

        // Balance should be restored
        assertEquals(new BigDecimal("500000"), feexpayAccount.getBalance());
        assertEquals(BigDecimal.ZERO, feexpayAccount.getReservedBalance());
    }

    @Test
    void testConfirmReservation_Success() {
        // Given
        BigDecimal amount = new BigDecimal("100000");
        when(operatorAccountRepository.findByProviderAndCurrencyWithLock(
                OperatorAccount.Provider.FEEXPAY, "XAF"))
                .thenReturn(Optional.of(feexpayAccount));

        String reservationId = liquidityManager.reserveForPayout(
                OperatorAccount.Provider.FEEXPAY, "XAF", amount);

        // When
        liquidityManager.confirmReservation(reservationId);

        // Then
        verify(operatorAccountRepository, times(2)).save(any(OperatorAccount.class));

        // Reserved balance should be cleared, actual balance stays reduced
        assertEquals(new BigDecimal("400000"), feexpayAccount.getBalance());
        assertEquals(BigDecimal.ZERO, feexpayAccount.getReservedBalance());
    }

    @Test
    void testGetAvailableBalance() {
        // Given
        when(operatorAccountRepository.findByProviderAndCurrency(
                OperatorAccount.Provider.FEEXPAY, "XAF"))
                .thenReturn(Optional.of(feexpayAccount));

        // When
        BigDecimal balance = liquidityManager.getAvailableBalance(
                OperatorAccount.Provider.FEEXPAY, "XAF");

        // Then
        assertEquals(new BigDecimal("500000"), balance);
    }

    @Test
    void testSuggestRebalancing() {
        // Given
        // This test is basic since rebalancing logic is simple threshold-based

        // When
        RebalanceSuggestion suggestions = liquidityManager.suggestRebalancing();

        // Then
        assertNotNull(suggestions);
        assertNotNull(suggestions.getActions());
        assertNotNull(suggestions.getAnalysisTimestamp());
    }
}
