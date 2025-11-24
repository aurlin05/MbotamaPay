package com.mbotamapay.backend.service;

import com.mbotamapay.backend.entity.KycDocument;
import com.mbotamapay.backend.repository.KycDocumentRepository;
import com.mbotamapay.backend.repository.TransactionRepository;
import com.mbotamapay.backend.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final KycDocumentRepository kycDocumentRepository;

    public DashboardStats getStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActiveTrue();
        long pendingKyc = kycDocumentRepository.findByStatus(KycDocument.DocumentStatus.PENDING).size();

        // Assuming we want total volume of successful transactions
        BigDecimal totalVolume = transactionRepository.calculateTotalVolume();
        if (totalVolume == null)
            totalVolume = BigDecimal.ZERO;

        return DashboardStats.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .pendingKycRequests(pendingKyc)
                .totalTransactionVolume(totalVolume)
                .build();
    }

    @Data
    @Builder
    public static class DashboardStats {
        private long totalUsers;
        private long activeUsers;
        private long pendingKycRequests;
        private BigDecimal totalTransactionVolume;
    }
}
