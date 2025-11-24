package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /**
     * Find all ledger entries for a specific reference ID
     * (typically returns 2 entries for double-entry transactions)
     */
    List<LedgerEntry> findByReferenceId(String referenceId);

    /**
     * Find all ledger entries for a specific wallet, ordered by creation date
     */
    Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    /**
     * Find all ledger entries for a specific wallet
     */
    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    /**
     * Calculate total balance for a wallet from ledger entries
     * CREDIT entries increase balance, DEBIT entries decrease balance
     */
    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN l.entryType = 'CREDIT' THEN l.amount ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN l.entryType = 'DEBIT' THEN l.amount ELSE 0 END), 0) " +
            "FROM LedgerEntry l WHERE l.wallet.id = :walletId AND l.status = 'SUCCESS'")
    BigDecimal calculateWalletBalance(@Param("walletId") Long walletId);

    /**
     * Check if a reference ID already exists (for idempotency)
     */
    boolean existsByReferenceId(String referenceId);
}
