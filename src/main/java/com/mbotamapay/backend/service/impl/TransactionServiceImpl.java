package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.transaction.TransferRequest;
import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.entity.*;
import com.mbotamapay.backend.event.TransactionCompletedEvent;
import com.mbotamapay.backend.exception.InsufficientBalanceException;
import com.mbotamapay.backend.exception.UserNotFoundException;
import com.mbotamapay.backend.repository.TransactionRepository;
import com.mbotamapay.backend.repository.UserRepository;
import com.mbotamapay.backend.repository.WalletRepository;
import com.mbotamapay.backend.service.EmailService;
import com.mbotamapay.backend.service.IdempotencyService;
import com.mbotamapay.backend.service.TransactionService;
import com.mbotamapay.backend.service.WalletService;
import com.mbotamapay.backend.utils.TransactionLimitValidator;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

        private final WalletRepository walletRepository;
        private final WalletService walletService;
        private final TransactionRepository transactionRepository;
        private final UserRepository userRepository;
        private final ApplicationEventPublisher eventPublisher;
        private final TransactionLimitValidator limitValidator;
        private final Counter transactionCounter;
        private final Counter p2pCounter;
        private final EmailService emailService;
        private final IdempotencyService idempotencyService;
        private final com.mbotamapay.backend.service.CacheService cacheService;
        private final com.mbotamapay.backend.service.AuditService auditService;

        @Override
        @Transactional
        public TransactionResponse sendMoney(User sender, TransferRequest request) {
                // Check idempotency if key is provided
                if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
                        // Check if this request was already processed
                        var existingResult = idempotencyService.checkIdempotency(request.getIdempotencyKey());
                        if (existingResult.isPresent()) {
                                log.info("Returning cached result for idempotency key: {}", request.getIdempotencyKey());
                                return existingResult.get();
                        }
                        
                        // Check if request is currently being processed
                        // Wait for up to 30 seconds for the original request to complete
                        int maxWaitAttempts = 30;
                        int waitAttempt = 0;
                        while (idempotencyService.isProcessing(request.getIdempotencyKey()) && waitAttempt < maxWaitAttempts) {
                                log.info("Request with idempotency key {} is being processed, waiting... (attempt {}/{})", 
                                        request.getIdempotencyKey(), waitAttempt + 1, maxWaitAttempts);
                                try {
                                        Thread.sleep(1000); // Wait 1 second
                                        waitAttempt++;
                                        
                                        // Check if result is now available
                                        var result = idempotencyService.checkIdempotency(request.getIdempotencyKey());
                                        if (result.isPresent()) {
                                                log.info("Original request completed, returning result for idempotency key: {}", 
                                                        request.getIdempotencyKey());
                                                return result.get();
                                        }
                                } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        throw new IllegalStateException("Interrupted while waiting for transaction to complete", e);
                                }
                        }
                        
                        // If still processing after max wait, throw exception
                        if (idempotencyService.isProcessing(request.getIdempotencyKey())) {
                                log.error("Request with idempotency key {} is still being processed after {} seconds", 
                                        request.getIdempotencyKey(), maxWaitAttempts);
                                throw new IllegalStateException("Transaction is taking too long to process. Please try again later.");
                        }
                        
                        // Mark as processing
                        idempotencyService.markAsProcessing(request.getIdempotencyKey());
                }
                
                try {
                        // Process the transaction with idempotency key
                        TransactionResponse response = processTransactionWithIdempotency(
                                sender, 
                                request.getRecipientIdentifier(), 
                                request.getAmount(), 
                                request.getDescription(),
                                request.getIdempotencyKey()
                        );
                        
                        // Store the result for idempotency if key was provided
                        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
                                idempotencyService.storeIdempotencyResult(request.getIdempotencyKey(), response);
                        }
                        
                        return response;
                } catch (Exception e) {
                        log.error("Error processing transaction with idempotency key: {}", request.getIdempotencyKey(), e);
                        // Note: We don't clean up the PROCESSING state here because Redis TTL will handle it
                        // This prevents race conditions where cleanup might interfere with retry logic
                        throw e;
                }
        }
        
        /**
         * Process a transaction with optional idempotency key.
         * This method is extracted to allow setting the idempotency key during transaction creation.
         */
        private TransactionResponse processTransactionWithIdempotency(User sender, String recipientEmail, 
                        BigDecimal amount, String description, String idempotencyKey) {
                
                log.info("Processing P2P transfer: {} -> {} for {} XAF", sender.getEmail(), recipientEmail, amount);

                // Validate transaction limit based on KYC
                limitValidator.validateTransactionLimit(sender, amount);

                User recipient = userRepository.findByEmail(recipientEmail)
                                .or(() -> userRepository.findByPhone(recipientEmail))
                                .orElseThrow(() -> new UserNotFoundException("Recipient not found"));

                if (sender.getId().equals(recipient.getId())) {
                        throw new IllegalArgumentException("Cannot send money to yourself");
                }

                Wallet senderWallet = walletRepository.findByUser(sender)
                                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));

                Wallet receiverWallet = walletRepository.findByUser(recipient)
                                .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

                // Calculate Fee (2%)
                BigDecimal feePercentage = new BigDecimal("0.02");
                BigDecimal fee = amount.multiply(feePercentage);
                BigDecimal totalDebit = amount.add(fee);

                // Check sufficient balance (Amount + Fee)
                if (senderWallet.getBalance().compareTo(totalDebit) < 0) {
                        throw new InsufficientBalanceException("Insufficient balance to cover amount + fee");
                }

                // Debit sender
                walletService.debit(senderWallet.getId(), totalDebit);

                // Credit receiver
                walletService.credit(receiverWallet.getId(), amount);

                // Create transaction record with idempotency key
                Transaction transaction = Transaction.builder()
                                .reference(UUID.randomUUID().toString())
                                .idempotencyKey(idempotencyKey) // Set idempotency key during creation
                                .senderWallet(senderWallet)
                                .receiverWallet(receiverWallet)
                                .amount(amount)
                                .fee(fee)
                                .type(TransactionType.P2P_TRANSFER)
                                .status(TransactionStatus.SUCCESS)
                                .description(description)
                                .build();

                Transaction saved = transactionRepository.save(transaction);

                // Log transaction to audit log
                auditService.logTransaction(saved, sender);

                // Handle Cashback (0.5%)
                BigDecimal cashbackPercentage = new BigDecimal("0.005");
                BigDecimal cashback = amount.multiply(cashbackPercentage);
                if (cashback.compareTo(BigDecimal.ZERO) > 0) {
                        walletService.credit(senderWallet.getId(), cashback);
                        // Log cashback transaction
                        Transaction cashbackTx = Transaction.builder()
                                        .reference(UUID.randomUUID().toString())
                                        .receiverWallet(senderWallet) // Credit to sender
                                        .amount(cashback)
                                        .type(TransactionType.CASHBACK)
                                        .status(TransactionStatus.SUCCESS)
                                        .description("Cashback for transaction " + saved.getReference())
                                        .build();
                        Transaction savedCashback = transactionRepository.save(cashbackTx);
                        auditService.logTransaction(savedCashback, sender);
                }

                // Handle Referral Commission (10% of Fee)
                if (sender.getReferrer() != null) {
                        BigDecimal referralSharePercentage = new BigDecimal("0.10");
                        BigDecimal referralBonus = fee.multiply(referralSharePercentage);

                        if (referralBonus.compareTo(BigDecimal.ZERO) > 0) {
                                Wallet referrerWallet = walletRepository.findByUser(sender.getReferrer()).orElse(null);

                                if (referrerWallet != null) {
                                        walletService.credit(referrerWallet.getId(), referralBonus);

                                        Transaction referralTx = Transaction.builder()
                                                        .reference(UUID.randomUUID().toString())
                                                        .receiverWallet(referrerWallet)
                                                        .amount(referralBonus)
                                                        .type(TransactionType.REFERRAL_BONUS)
                                                        .status(TransactionStatus.SUCCESS)
                                                        .description("Referral bonus from " + sender.getName())
                                                        .build();
                                        Transaction savedReferral = transactionRepository.save(referralTx);
                                        auditService.logTransaction(savedReferral, sender);
                                }
                        }
                }

                // Send Receipt Email
                String receiptContent = String.format(
                                "<h1>Transaction Successful</h1>" +
                                                "<p>You sent <b>%s XAF</b> to <b>%s</b>.</p>" +
                                                "<p>Transaction Reference: %s</p>" +
                                                "<p>Fee: %s XAF</p>" +
                                                "<p>Date: %s</p>",
                                amount, receiverWallet.getUser().getName(), saved.getReference(), fee,
                                java.time.LocalDateTime.now());
                emailService.sendEmail(sender.getEmail(), "Transaction Receipt - MbotamaPay", receiptContent);

                // Increment metrics
                transactionCounter.increment();
                p2pCounter.increment();

                // Publish event
                eventPublisher.publishEvent(new TransactionCompletedEvent(this, saved));

                // Invalidate transaction history cache for both sender and receiver
                cacheService.evictTransactionHistoryCache(sender.getId());
                cacheService.evictTransactionHistoryCache(recipient.getId());
                
                // Also invalidate for referrer if applicable
                if (sender.getReferrer() != null) {
                        cacheService.evictTransactionHistoryCache(sender.getReferrer().getId());
                }

                return TransactionResponse.builder()
                                .reference(saved.getReference())
                                .status(saved.getStatus().name())
                                .amount(saved.getAmount())
                                .build();
        }

        @Override
        @Transactional
        public TransactionResponse sendMoneyByEmail(User sender, String recipientEmail, BigDecimal amount,
                        String description) {
                // Delegate to the method that handles idempotency
                return processTransactionWithIdempotency(sender, recipientEmail, amount, description, null);
        }

        @Override
        @Transactional(readOnly = true)
        @org.springframework.cache.annotation.Cacheable(value = "transactionHistory", key = "#user.id")
        public java.util.List<TransactionResponse> getTransactionHistory(User user) {
                Wallet wallet = walletRepository.findByUser(user)
                                .orElseThrow(() -> new RuntimeException("Wallet not found"));

                return transactionRepository.findBySenderWalletOrReceiverWalletOrderByCreatedAtDesc(wallet, wallet)
                                .stream()
                                .map(this::toResponse)
                                .collect(java.util.stream.Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        @org.springframework.cache.annotation.Cacheable(value = "transactionHistory", key = "#user.id + '_' + #pageable.pageNumber")
        public Page<TransactionResponse> getTransactionHistoryPaginated(User user, Pageable pageable) {
                Wallet wallet = walletRepository.findByUser(user)
                                .orElseThrow(() -> new RuntimeException("Wallet not found"));

                return transactionRepository
                                .findBySenderWalletOrReceiverWalletOrderByCreatedAtDesc(wallet, wallet, pageable)
                                .map(this::toResponse);
        }

        private TransactionResponse toResponse(Transaction transaction) {
                return TransactionResponse.builder()
                                .id(transaction.getId())
                                .reference(transaction.getReference())
                                .amount(transaction.getAmount())
                                .type(transaction.getType().name())
                                .status(transaction.getStatus().name())
                                .senderEmail(transaction.getSenderWallet() != null
                                                ? transaction.getSenderWallet().getUser().getEmail()
                                                : null)
                                .receiverEmail(
                                                transaction.getReceiverWallet() != null
                                                                ? transaction.getReceiverWallet().getUser().getEmail()
                                                                : null)
                                .description(transaction.getDescription())
                                .createdAt(transaction.getCreatedAt())
                                .build();
        }
}
