package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.entity.Wallet;
import com.mbotamapay.backend.repository.WalletRepository;
import com.mbotamapay.backend.utils.ConcurrentModificationHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private ConcurrentModificationHandler concurrentModificationHandler;

    @Mock
    private com.mbotamapay.backend.service.CacheService cacheService;

    @Mock
    private com.mbotamapay.backend.service.AuditService auditService;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void credit_ShouldIncreaseBalance() {
        // Given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();
        
        Wallet wallet = Wallet.builder()
                .id(1L)
                .balance(new BigDecimal("1000"))
                .user(user)
                .build();

        when(walletRepository.findById(1L)).thenReturn(java.util.Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(concurrentModificationHandler.retryOnOptimisticLock(any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

        // When
        walletService.credit(1L, new BigDecimal("500"));

        // Then
        verify(walletRepository, times(1)).save(wallet);
        verify(cacheService, times(1)).evictWalletCache(1L);
        verify(auditService, times(1)).logWalletModification(
                eq(wallet), 
                eq(new BigDecimal("1000")), 
                eq(new BigDecimal("1500")), 
                eq(user)
        );
        assertEquals(new BigDecimal("1500"), wallet.getBalance());
    }

    @Test
    void debit_ShouldDecreaseBalance() {
        // Given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();
        
        Wallet wallet = Wallet.builder()
                .id(1L)
                .balance(new BigDecimal("1000"))
                .user(user)
                .build();

        when(walletRepository.findById(1L)).thenReturn(java.util.Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(concurrentModificationHandler.retryOnOptimisticLock(any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

        // When
        walletService.debit(1L, new BigDecimal("300"));

        // Then
        verify(walletRepository, times(1)).save(wallet);
        verify(cacheService, times(1)).evictWalletCache(1L);
        verify(auditService, times(1)).logWalletModification(
                eq(wallet), 
                eq(new BigDecimal("1000")), 
                eq(new BigDecimal("700")), 
                eq(user)
        );
        assertEquals(new BigDecimal("700"), wallet.getBalance());
    }

    @Test
    void debit_ShouldThrowException_WhenInsufficientBalance() {
        // Given
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();
        
        Wallet wallet = Wallet.builder()
                .id(1L)
                .balance(new BigDecimal("100"))
                .user(user)
                .build();

        when(walletRepository.findById(1L)).thenReturn(java.util.Optional.of(wallet));
        when(concurrentModificationHandler.retryOnOptimisticLock(any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

        // When/Then
        assertThrows(Exception.class, () -> walletService.debit(1L, new BigDecimal("500")));
        
        // Verify audit logging was NOT called since the operation failed
        verify(auditService, never()).logWalletModification(any(), any(), any(), any());
    }
}
