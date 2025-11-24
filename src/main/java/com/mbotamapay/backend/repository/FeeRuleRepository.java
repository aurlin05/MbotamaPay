package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.FeeRule;
import com.mbotamapay.backend.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRuleRepository extends JpaRepository<FeeRule, Long> {

    /**
     * Find active fee rule for provider and transaction type
     */
    Optional<FeeRule> findByProviderAndTransactionTypeAndActiveTrue(
            String provider,
            TransactionType transactionType);

    /**
     * Find all fee rules for a provider
     */
    List<FeeRule> findByProvider(String provider);

    /**
     * Find all active fee rules
     */
    List<FeeRule> findByActiveTrue();

    /**
     * Find all fee rules for a transaction type
     */
    List<FeeRule> findByTransactionType(TransactionType transactionType);

    /**
     * Find fee rule by provider, transaction type, and currency
     */
    @Query("SELECT fr FROM FeeRule fr " +
            "WHERE fr.provider = :provider " +
            "AND fr.transactionType = :transactionType " +
            "AND fr.currency = :currency " +
            "AND fr.active = true")
    Optional<FeeRule> findActiveByProviderAndTypeAndCurrency(
            @Param("provider") String provider,
            @Param("transactionType") TransactionType transactionType,
            @Param("currency") String currency);
}
