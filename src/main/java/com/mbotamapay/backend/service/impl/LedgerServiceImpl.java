package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.entity.LedgerEntry;
import com.mbotamapay.backend.entity.Transaction;
import com.mbotamapay.backend.entity.Wallet;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.repository.LedgerEntryRepository;
import com.mbotamapay.backend.repository.WalletRepository;
import com.mbotamapay.backend.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerServiceImpl implements LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordTransaction(Transaction transaction, Wallet fromWallet, Wallet toWallet, BigDecimal amount,
            String currency, String metadata) {
        log.info("Recording ledger transaction: {} from {} to {}", transaction.getReference(), fromWallet.getId(),
                toWallet.getId());

        // 1. Debit Sender
        LedgerEntry debitEntry = LedgerEntry.builder()
                .referenceId(transaction.getReference())
                .wallet(fromWallet)
                .amount(amount)
                .currency(currency)
                .entryType(LedgerEntry.EntryType.DEBIT)
                .transactionType(transaction.getType())
                .status(LedgerEntry.LedgerStatus.SUCCESS)
                .metadata(metadata)
                .build();
        ledgerEntryRepository.save(debitEntry);

        // Update sender wallet balance
        // Note: Wallet balance update should ideally happen here or be verified here
        // We assume the caller (TransactionService) might have already locked/updated
        // the wallet,
        // but for consistency, we should ensure the wallet state reflects this.
        // However, if we update here, we must ensure we don't double count if
        // WalletService also updates.
        // The plan is to migrate WalletService to use LedgerService.
        // For now, we'll update the wallet balance here to ensure ACID.

        BigDecimal newFromBalance = fromWallet.getBalance().subtract(amount);
        if (newFromBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Insufficient funds in wallet " + fromWallet.getId());
        }
        fromWallet.setBalance(newFromBalance);
        walletRepository.save(fromWallet);

        // 2. Credit Receiver
        LedgerEntry creditEntry = LedgerEntry.builder()
                .referenceId(transaction.getReference())
                .wallet(toWallet)
                .amount(amount)
                .currency(currency)
                .entryType(LedgerEntry.EntryType.CREDIT)
                .transactionType(transaction.getType())
                .status(LedgerEntry.LedgerStatus.SUCCESS)
                .metadata(metadata)
                .build();
        ledgerEntryRepository.save(creditEntry);

        // Update receiver wallet balance
        BigDecimal newToBalance = toWallet.getBalance().add(amount);
        toWallet.setBalance(newToBalance);
        walletRepository.save(toWallet);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEntry(Transaction transaction, Wallet wallet, BigDecimal amount, LedgerEntry.EntryType type,
            String currency, String metadata) {
        log.info("Recording ledger entry: {} for wallet {} type {}", transaction.getReference(), wallet.getId(), type);

        LedgerEntry entry = LedgerEntry.builder()
                .referenceId(transaction.getReference())
                .wallet(wallet)
                .amount(amount)
                .currency(currency)
                .entryType(type)
                .transactionType(transaction.getType())
                .status(LedgerEntry.LedgerStatus.SUCCESS)
                .metadata(metadata)
                .build();
        ledgerEntryRepository.save(entry);

        // Update wallet balance
        if (type == LedgerEntry.EntryType.DEBIT) {
            BigDecimal newBalance = wallet.getBalance().subtract(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Insufficient funds in wallet " + wallet.getId());
            }
            wallet.setBalance(newBalance);
        } else {
            wallet.setBalance(wallet.getBalance().add(amount));
        }
        walletRepository.save(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyWalletBalance(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new BusinessException("Wallet not found"));

        BigDecimal calculatedBalance = ledgerEntryRepository.calculateWalletBalance(walletId);

        // Handle null if no entries
        if (calculatedBalance == null) {
            calculatedBalance = BigDecimal.ZERO;
        }

        boolean matches = wallet.getBalance().compareTo(calculatedBalance) == 0;
        if (!matches) {
            log.error("Balance mismatch for wallet {}: Wallet={}, Ledger={}", walletId, wallet.getBalance(),
                    calculatedBalance);
        }
        return matches;
    }

    @Override
    public List<LedgerEntry> getEntriesByReference(String referenceId) {
        return ledgerEntryRepository.findByReferenceId(referenceId);
    }
}
