package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * Find exchange rate for a specific currency pair
     */
    Optional<ExchangeRate> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);

    /**
     * Find all exchange rates from a specific currency
     */
    List<ExchangeRate> findByFromCurrency(String fromCurrency);

    /**
     * Find all exchange rates to a specific currency
     */
    List<ExchangeRate> findByToCurrency(String toCurrency);

    /**
     * Check if exchange rate exists for currency pair
     */
    boolean existsByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);

    /**
     * Get all supported currencies (distinct from both from and to)
     */
    @Query("SELECT DISTINCT e.fromCurrency FROM ExchangeRate e " +
            "UNION " +
            "SELECT DISTINCT e.toCurrency FROM ExchangeRate e")
    List<String> findAllSupportedCurrencies();
}
