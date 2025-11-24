package com.mbotamapay.backend.service;

import com.mbotamapay.backend.entity.Transaction;
import com.mbotamapay.backend.entity.Wallet;
import com.mbotamapay.backend.entity.LedgerEntry;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerService {

    /**
     * Record a double-entry transaction in the ledger.
     * This method handles creating both DEBIT and CREDIT entries atomically.
     * 
     * @param transaction The parent transaction
     * @param fromWallet  Source wallet (to be debited)
     * @param toWallet    Destination wallet (to be credited)
     * @param amount      Amount to transfer
     * @param currency    Currency of the transaction
     * @param metadata    Additional metadata
     */
    void recordTransaction(Transaction transaction, Wallet fromWallet, Wallet toWallet, BigDecimal amount,
            String currency, String metadata);

    /**
     * Record a single-sided entry (e.g., external deposit/withdrawal where only one
     * internal wallet is involved).
     * Note: In a true double-entry system, the other side would be an
     * external/system account.
     * For now, we record the user side.
     * 
     * @param transaction The parent transaction
     * @param wallet      Wallet to affect
     * @param amount      Amount
     * @param type        DEBIT or CREDIT
     * @param currency    Currency
     * @param metadata    Metadata
     */
    void recordEntry(Transaction transaction, Wallet wallet, BigDecimal amount, LedgerEntry.EntryType type,
            String currency, String metadata);

    /**
     * Verify wallet balance against ledger entries.
     * Returns true if wallet.balance matches sum of ledger entries.
     */
    boolean verifyWalletBalance(Long walletId);

    /**
     * Get all ledger entries for a transaction reference
     */
    List<LedgerEntry> getEntriesByReference(String referenceId);
}
