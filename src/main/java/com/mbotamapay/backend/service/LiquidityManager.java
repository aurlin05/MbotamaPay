package com.mbotamapay.backend.service;

import com.mbotamapay.backend.dto.liquidity.RebalanceSuggestion;
import com.mbotamapay.backend.entity.OperatorAccount;

import java.math.BigDecimal;

public interface LiquidityManager {

    /**
     * Reserve funds for a payout
     * 
     * @param provider Provider to reserve from
     * @param currency Currency
     * @param amount   Amount to reserve
     * @return Reservation ID (UUID)
     */
    String reserveForPayout(OperatorAccount.Provider provider, String currency, BigDecimal amount);

    /**
     * Release reservation (rollback)
     * 
     * @param reservationId Reservation ID
     */
    void releaseReservation(String reservationId);

    /**
     * Confirm reservation (commit)
     * 
     * @param reservationId Reservation ID
     */
    void confirmReservation(String reservationId);

    /**
     * Get available balance for a provider
     */
    BigDecimal getAvailableBalance(OperatorAccount.Provider provider, String currency);

    /**
     * Suggest rebalancing actions based on current balances and history
     */
    RebalanceSuggestion suggestRebalancing();
}
