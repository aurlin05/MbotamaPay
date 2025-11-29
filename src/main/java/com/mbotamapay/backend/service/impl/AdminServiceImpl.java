package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.admin.AdminStatsResponse;
import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.dto.user.UserResponse;
import com.mbotamapay.backend.entity.Transaction;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.entity.Wallet;
import com.mbotamapay.backend.repository.TransactionRepository;
import com.mbotamapay.backend.repository.UserRepository;
import com.mbotamapay.backend.repository.WalletRepository;
import com.mbotamapay.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

        private final UserRepository userRepository;
        private final TransactionRepository transactionRepository;
        private final WalletRepository walletRepository;
        private final com.mbotamapay.backend.service.AuditService auditService;

        @Override
        @Transactional(readOnly = true)
        public List<UserResponse> getAllUsers() {
                return userRepository.findAll().stream()
                                .map(this::mapToUserResponse)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<TransactionResponse> getAllTransactions() {
                return transactionRepository.findAll().stream()
                                .map(this::mapToTransactionResponse)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "adminStats")
        public AdminStatsResponse getStats() {
                long totalUsers = userRepository.count();
                long activeUsers = userRepository.findAll().stream()
                                .filter(User::isActive)
                                .count();

                long totalTransactions = transactionRepository.count();

                BigDecimal totalFunds = walletRepository.findAll().stream()
                                .map(Wallet::getBalance)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return AdminStatsResponse.builder()
                                .totalUsers(totalUsers)
                                .activeUsers(activeUsers)
                                .totalTransactions(totalTransactions)
                                .totalFundsCirculating(totalFunds)
                                .build();
        }

        @Override
        @Transactional
        public void banUser(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                user.setActive(false);
                userRepository.save(user);

                // Log admin action
                User admin = getCurrentAdmin();
                java.util.Map<String, Object> details = new java.util.HashMap<>();
                details.put("action", "BAN_USER");
                details.put("targetUserId", userId);
                details.put("targetUserEmail", user.getEmail());
                auditService.logAdminAction("BAN_USER", admin, details);

                log.info("User {} banned by admin {}", userId, admin != null ? admin.getEmail() : "SYSTEM");
        }

        @Override
        @Transactional
        public void unbanUser(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                user.setActive(true);
                userRepository.save(user);

                // Log admin action
                User admin = getCurrentAdmin();
                java.util.Map<String, Object> details = new java.util.HashMap<>();
                details.put("action", "UNBAN_USER");
                details.put("targetUserId", userId);
                details.put("targetUserEmail", user.getEmail());
                auditService.logAdminAction("UNBAN_USER", admin, details);

                log.info("User {} unbanned by admin {}", userId, admin != null ? admin.getEmail() : "SYSTEM");
        }

        /**
         * Get the current authenticated admin user from SecurityContext
         */
        private User getCurrentAdmin() {
                try {
                        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                                        .getContext().getAuthentication();
                        if (authentication != null && authentication
                                        .getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                                org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) authentication
                                                .getPrincipal();

                                // If using CustomUserDetails, we can get the User object directly
                                if (userDetails instanceof com.mbotamapay.backend.security.CustomUserDetails) {
                                        return ((com.mbotamapay.backend.security.CustomUserDetails) userDetails)
                                                        .getUser();
                                }
                        }
                } catch (Exception e) {
                        log.debug("Could not retrieve current admin from SecurityContext", e);
                }
                return null;
        }

        private UserResponse mapToUserResponse(User user) {
                return UserResponse.builder()
                                .id(user.getId())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .role(user.getRole().name())
                                .kycLevel(user.getKycLevel().name())
                                .active(user.isActive())
                                .createdAt(user.getCreatedAt())
                                .build();
        }

        private TransactionResponse mapToTransactionResponse(Transaction transaction) {
                return TransactionResponse.builder()
                                .id(transaction.getId())
                                .reference(transaction.getReference())
                                .type(transaction.getType().name())
                                .status(transaction.getStatus().name())
                                .amount(transaction.getAmount())
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
