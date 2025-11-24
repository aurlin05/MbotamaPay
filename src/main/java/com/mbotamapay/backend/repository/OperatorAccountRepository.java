package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.OperatorAccount;
import com.mbotamapay.backend.entity.OperatorAccount.Provider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorAccountRepository extends JpaRepository<OperatorAccount, Long> {

    /**
     * Find operator account by provider and currency
     */
    Optional<OperatorAccount> findByProviderAndCurrency(Provider provider, String currency);

    /**
     * Find operator account by provider and currency with pessimistic write lock
     * Used for atomic balance updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT oa FROM OperatorAccount oa WHERE oa.provider = :provider AND oa.currency = :currency")
    Optional<OperatorAccount> findByProviderAndCurrencyWithLock(
            @Param("provider") Provider provider,
            @Param("currency") String currency);

    /**
     * Find all operator accounts for a specific provider
     */
    List<OperatorAccount> findByProvider(Provider provider);

    /**
     * Find all operator accounts for a specific currency
     */
    List<OperatorAccount> findByCurrency(String currency);

    /**
     * Check if operator account exists for provider and currency
     */
    boolean existsByProviderAndCurrency(Provider provider, String currency);
}
