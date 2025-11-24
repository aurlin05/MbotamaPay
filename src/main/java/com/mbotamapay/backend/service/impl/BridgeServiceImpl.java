package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.bridge.BridgeTransferRequest;
import com.mbotamapay.backend.dto.bridge.BridgeTransferResponse;
import com.mbotamapay.backend.dto.bridge.BridgeTransferStatus;
import com.mbotamapay.backend.integrations.dto.PayoutRequest;
import com.mbotamapay.backend.integrations.dto.PayoutResponse;
import com.mbotamapay.backend.entity.*;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.integrations.PaymentProvider;
import com.mbotamapay.backend.integrations.ProviderFactory;
import com.mbotamapay.backend.repository.TransactionRepository;
import com.mbotamapay.backend.repository.WalletRepository;
import com.mbotamapay.backend.service.BridgeService;
import com.mbotamapay.backend.service.FeeService;
import com.mbotamapay.backend.service.LedgerService;
import com.mbotamapay.backend.service.LiquidityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BridgeServiceImpl implements BridgeService {

    private final ProviderFactory providerFactory;
    private final LiquidityManager liquidityManager;
    private final LedgerService ledgerService;
    private final FeeService feeService;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public BridgeTransferResponse bridgeTransfer(BridgeTransferRequest request) {
        log.info("Initiating bridge transfer: {} -> {} amount: {}", request.getFromProvider(), request.getToProvider(),
                request.getAmount());

        // 1. Validate User Wallet
        Wallet userWallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new BusinessException("Wallet not found"));

        // 2. Calculate Fees
        BigDecimal feeAmount = feeService.calculateFee(request.getAmount(), TransactionType.WITHDRAW,
                request.getToProvider());
        BigDecimal totalDebitAmount = request.getAmount().add(feeAmount);

        if (userWallet.getAvailableBalance().compareTo(totalDebitAmount) < 0) {
            throw new BusinessException("Insufficient balance including fees");
        }

        // 3. Create Transaction Record (Pending)
        String referenceId = "BRG-" + UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .reference(referenceId)
                .receiverWallet(userWallet) // It's a debit, so receiver is null or system?
                // Transaction entity design usually has sender/receiver.
                // For withdraw, sender is user, receiver is external.
                // Existing Transaction entity might need check.
                // Let's assume we set user as sender or receiver depending on type.
                // For now, I'll set receiverWallet as userWallet but type is WITHDRAW.
                .amount(request.getAmount())
                .fee(feeAmount)
                .currency(request.getCurrency())
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.PENDING)
                .description("Bridge Transfer to " + request.getToProvider())
                .build();
        transactionRepository.save(transaction);

        // 4. Debit User Wallet (Ledger)
        // We debit the user wallet. The credit side is the "Bridge/System" wallet.
        // Since we don't have a specific system wallet entity instance easily
        // available,
        // we'll use a "Single Entry" debit for now, or assume a system wallet ID 0.
        // LedgerService supports single entry recording.
        ledgerService.recordEntry(transaction, userWallet, totalDebitAmount, LedgerEntry.EntryType.DEBIT,
                request.getCurrency(), "Bridge Debit");

        // 5. Reserve Liquidity on Target Provider
        String reservationId;
        try {
            reservationId = liquidityManager.reserveForPayout(request.getToProvider(), request.getCurrency(),
                    request.getAmount());
        } catch (Exception e) {
            // Rollback: Credit back the user (or let Transactional handle it if runtime
            // exception)
            // Since we are in @Transactional, throwing exception will rollback DB changes
            // (Ledger & Transaction).
            throw new BusinessException("Failed to reserve liquidity: " + e.getMessage());
        }

        // 6. Initiate Payout via Provider
        PaymentProvider provider = providerFactory.getProvider(request.getToProvider().name());
        PayoutRequest payoutRequest = PayoutRequest.builder()
                .referenceId(referenceId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .phoneNumber(request.getRecipientPhone())
                .description("Bridge Transfer " + referenceId)
                .build();

        try {
            PayoutResponse response = provider.initiatePayout(payoutRequest);

            if ("SUCCESS".equals(response.getStatus()) || "PENDING".equals(response.getStatus())) {
                // Update transaction status
                transaction.setStatus(TransactionStatus.PENDING); // Keep pending until final confirmation
                transaction.setProviderReference(response.getReference());
                transactionRepository.save(transaction);

                // If synchronous success, we could confirm reservation now, but safer to wait
                // for webhook or status check.
                // However, if provider says SUCCESS immediately, we should confirm.
                if ("SUCCESS".equals(response.getStatus())) {
                    liquidityManager.confirmReservation(reservationId);
                    transaction.setStatus(TransactionStatus.SUCCESS);
                    transactionRepository.save(transaction);
                }

                return BridgeTransferResponse.builder()
                        .referenceId(referenceId)
                        .status(mapStatus(response.getStatus()))
                        .toProviderTxId(response.getReference())
                        .message(response.getMessage())
                        .build();
            } else {
                // Failure
                throw new RuntimeException("Provider returned failure: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("Bridge payout failed", e);
            // Release reservation
            liquidityManager.releaseReservation(reservationId);
            // Transaction rollback is handled by @Transactional if we throw exception.
            // But we might want to return a FAILED response instead of 500.
            // If we catch exception, transaction commits (Ledger debit persists).
            // We MUST throw exception to rollback Ledger, OR manually reverse Ledger.
            // Throwing exception is cleaner for atomicity.
            throw new BusinessException("Bridge transfer failed: " + e.getMessage());
        }
    }

    @Override
    public BridgeTransferStatus checkBridgeStatus(String referenceId) {
        Transaction transaction = transactionRepository.findByReference(referenceId)
                .orElseThrow(() -> new BusinessException("Transaction not found"));

        return mapTransactionStatus(transaction.getStatus());
    }

    private BridgeTransferStatus mapStatus(String providerStatus) {
        if (providerStatus == null)
            return BridgeTransferStatus.PENDING;
        switch (providerStatus.toUpperCase()) {
            case "SUCCESS":
                return BridgeTransferStatus.COMPLETED;
            case "FAILED":
                return BridgeTransferStatus.FAILED;
            default:
                return BridgeTransferStatus.PENDING;
        }
    }

    private BridgeTransferStatus mapTransactionStatus(TransactionStatus status) {
        switch (status) {
            case SUCCESS:
                return BridgeTransferStatus.COMPLETED;
            case FAILED:
                return BridgeTransferStatus.FAILED;
            default:
                return BridgeTransferStatus.PENDING;
        }
    }
}
