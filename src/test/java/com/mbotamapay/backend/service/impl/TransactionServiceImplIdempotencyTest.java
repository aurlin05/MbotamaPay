package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.dto.transaction.TransferRequest;
import com.mbotamapay.backend.entity.*;
import com.mbotamapay.backend.event.TransactionCompletedEvent;
import com.mbotamapay.backend.repository.TransactionRepository;
import com.mbotamapay.backend.repository.UserRepository;
import com.mbotamapay.backend.repository.WalletRepository;
import com.mbotamapay.backend.service.EmailService;
import com.mbotamapay.backend.service.IdempotencyService;
import com.mbotamapay.backend.service.WalletService;
import com.mbotamapay.backend.utils.TransactionLimitValidator;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionServiceImpl idempotency functionality.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplIdempotencyTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TransactionLimitValidator limitValidator;

    @Mock
    private Counter transactionCounter;

    @Mock
    private Counter p2pCounter;

    @Mock
    private EmailService emailService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private com.mbotamapay.backend.service.CacheService cacheService;

    @Mock
    private com.mbotamapay.backend.service.AuditService auditService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User sender;
    private User recipient;
    private Wallet senderWallet;
    private Wallet receiverWallet;
    private TransferRequest request;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id(1L)
                .email("sender@example.com")
                .name("Sender User")
                .build();

        recipient = User.builder()
                .id(2L)
                .email("recipient@example.com")
                .name("Recipient User")
                .build();

        senderWallet = Wallet.builder()
                .id(1L)
                .user(sender)
                .balance(new BigDecimal("1000.00"))
                .currency("XAF")
                .build();

        receiverWallet = Wallet.builder()
                .id(2L)
                .user(recipient)
                .balance(new BigDecimal("500.00"))
                .currency("XAF")
                .build();

        request = TransferRequest.builder()
                .recipientIdentifier("recipient@example.com")
                .amount(new BigDecimal("100.00"))
                .description("Test transfer")
                .idempotencyKey("test-idempotency-key-123")
                .build();
    }

    @Test
    void sendMoney_ShouldReturnCachedResult_WhenIdempotencyKeyExists() {
        // Arrange
        TransactionResponse cachedResponse = TransactionResponse.builder()
                .reference("cached-ref-123")
                .status("SUCCESS")
                .amount(new BigDecimal("100.00"))
                .build();

        when(idempotencyService.checkIdempotency("test-idempotency-key-123"))
                .thenReturn(Optional.of(cachedResponse));

        // Act
        TransactionResponse result = transactionService.sendMoney(sender, request);

        // Assert
        assertNotNull(result);
        assertEquals("cached-ref-123", result.getReference());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(new BigDecimal("100.00"), result.getAmount());

        // Verify that no transaction processing occurred
        verify(idempotencyService).checkIdempotency("test-idempotency-key-123");
        verify(idempotencyService, never()).markAsProcessing(anyString());
        verify(walletService, never()).debit(anyLong(), any(BigDecimal.class));
        verify(walletService, never()).credit(anyLong(), any(BigDecimal.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void sendMoney_ShouldProcessTransaction_WhenIdempotencyKeyIsNew() {
        // Arrange
        when(idempotencyService.checkIdempotency("test-idempotency-key-123"))
                .thenReturn(Optional.empty());
        when(idempotencyService.isProcessing("test-idempotency-key-123"))
                .thenReturn(false);

        when(userRepository.findByEmail("recipient@example.com"))
                .thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUser(recipient))
                .thenReturn(Optional.of(receiverWallet));

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .reference("new-ref-456")
                .idempotencyKey("test-idempotency-key-123")
                .senderWallet(senderWallet)
                .receiverWallet(receiverWallet)
                .amount(new BigDecimal("100.00"))
                .fee(new BigDecimal("2.00"))
                .type(TransactionType.P2P_TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .description("Test transfer")
                .build();

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        // Act
        TransactionResponse result = transactionService.sendMoney(sender, request);

        // Assert
        assertNotNull(result);
        assertEquals("new-ref-456", result.getReference());
        assertEquals("SUCCESS", result.getStatus());

        // Verify idempotency flow - key methods were called
        verify(idempotencyService, atLeastOnce()).checkIdempotency("test-idempotency-key-123");
        verify(idempotencyService, atLeastOnce()).isProcessing("test-idempotency-key-123");
        verify(idempotencyService).markAsProcessing("test-idempotency-key-123");
        verify(idempotencyService).storeIdempotencyResult(eq("test-idempotency-key-123"), any(TransactionResponse.class));

        // Verify transaction processing occurred
        verify(limitValidator).validateTransactionLimit(sender, new BigDecimal("100.00"));
    }

    @Test
    void sendMoney_ShouldProcessWithoutIdempotency_WhenKeyIsNull() {
        // Arrange
        TransferRequest requestWithoutKey = TransferRequest.builder()
                .recipientIdentifier("recipient@example.com")
                .amount(new BigDecimal("100.00"))
                .description("Test transfer")
                .idempotencyKey(null)
                .build();

        when(userRepository.findByEmail("recipient@example.com"))
                .thenReturn(Optional.of(recipient));
        when(walletRepository.findByUser(sender))
                .thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUser(recipient))
                .thenReturn(Optional.of(receiverWallet));

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .reference("new-ref-789")
                .senderWallet(senderWallet)
                .receiverWallet(receiverWallet)
                .amount(new BigDecimal("100.00"))
                .fee(new BigDecimal("2.00"))
                .type(TransactionType.P2P_TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .description("Test transfer")
                .build();

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        // Act
        TransactionResponse result = transactionService.sendMoney(sender, requestWithoutKey);

        // Assert
        assertNotNull(result);
        assertEquals("new-ref-789", result.getReference());

        // Verify idempotency service was not called
        verify(idempotencyService, never()).checkIdempotency(anyString());
        verify(idempotencyService, never()).markAsProcessing(anyString());
        verify(idempotencyService, never()).storeIdempotencyResult(anyString(), any());
    }

    @Test
    void sendMoney_ShouldThrowException_WhenTransactionIsProcessingTooLong() {
        // Arrange
        when(idempotencyService.checkIdempotency("test-idempotency-key-123"))
                .thenReturn(Optional.empty());
        when(idempotencyService.isProcessing("test-idempotency-key-123"))
                .thenReturn(true); // Always return true to simulate long processing

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            transactionService.sendMoney(sender, request);
        });

        assertTrue(exception.getMessage().contains("taking too long"));
        verify(idempotencyService, never()).markAsProcessing(anyString());
    }
}
