package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.entity.ExchangeRate;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.repository.ExchangeRateRepository;
import com.mbotamapay.backend.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        BigDecimal rate = getRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP); // Standard 2 decimals
    }

    @Override
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return BigDecimal.ONE;
        }

        // Try direct rate
        Optional<ExchangeRate> rateOpt = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency,
                toCurrency);
        if (rateOpt.isPresent()) {
            return rateOpt.get().getRate();
        }

        // Try inverse rate
        rateOpt = exchangeRateRepository.findByFromCurrencyAndToCurrency(toCurrency, fromCurrency);
        if (rateOpt.isPresent()) {
            // Inverse: 1 / rate
            return BigDecimal.ONE.divide(rateOpt.get().getRate(), 8, RoundingMode.HALF_UP);
        }

        log.error("No exchange rate found for {} -> {}", fromCurrency, toCurrency);
        throw new BusinessException("Exchange rate not available for " + fromCurrency + " to " + toCurrency);
    }
}
