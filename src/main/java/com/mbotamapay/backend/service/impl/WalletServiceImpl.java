package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.wallet.WalletResponse;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.entity.Wallet;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.repository.WalletRepository;
import com.mbotamapay.backend.service.CacheService;
import com.mbotamapay.backend.service.WalletService;
import com.mbotamapay.backend.utils.ConcurrentModificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final ConcurrentModificationHandler concurrentModificationHandler;
    private final CacheService cacheService;
    private final com.mbotamapay.backend.service.AuditService auditService;

    @Override
    @Transactional
    public Wallet createWallet(User user) {
        log.info("Creating wallet for user: {}", user.getEmail());
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .currency("XAF") // Default currency (CFA Franc)
                .build();
        return walletRepository.save(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "walletBalance", key = "#userId")
    public WalletResponse getBalance(Long userId) {
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Wallet not found"));

        return WalletResponse.builder()
                .id(wallet.getId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "walletBalance", key = "#walletId")
    public void credit(Long walletId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Credit amount must be positive");
        }

        concurrentModificationHandler.retryOnOptimisticLock(() -> {
            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new BusinessException("Wallet not found"));

            BigDecimal oldBalance = wallet.getBalance();
            wallet.setBalance(wallet.getBalance().add(amount));
            Wallet savedWallet = walletRepository.save(wallet);
            log.info("Credited {} to wallet {}", amount, walletId);
            
            // Log wallet modification to audit log
            auditService.logWalletModification(savedWallet, oldBalance, savedWallet.getBalance(), wallet.getUser());
            
            // Invalidate wallet cache after modification
            cacheService.evictWalletCache(walletId);
            
            return null;
        });
    }

    @Override
    @Transactional
    @CacheEvict(value = "walletBalance", key = "#walletId")
    public void debit(Long walletId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Debit amount must be positive");
        }

        concurrentModificationHandler.retryOnOptimisticLock(() -> {
            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new BusinessException("Wallet not found"));

            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new BusinessException("Insufficient balance");
            }

            BigDecimal oldBalance = wallet.getBalance();
            wallet.setBalance(wallet.getBalance().subtract(amount));
            Wallet savedWallet = walletRepository.save(wallet);
            log.info("Debited {} from wallet {}", amount, walletId);
            
            // Log wallet modification to audit log
            auditService.logWalletModification(savedWallet, oldBalance, savedWallet.getBalance(), wallet.getUser());
            
            // Invalidate wallet cache after modification
            cacheService.evictWalletCache(walletId);
            
            return null;
        });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wallets", key = "#user.id")
    public Wallet getWalletByUser(User user) {
        return walletRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Wallet not found for user"));
    }
}
