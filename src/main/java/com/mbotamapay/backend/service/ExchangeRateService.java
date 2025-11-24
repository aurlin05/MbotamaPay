package com.mbotamapay.backend.service;

import java.math.BigDecimal;

public interface ExchangeRateService {

    /**
     * Convert amount from one currency to another
     * 
     * @param amount       Amount to convert
     * @param fromCurrency Source currency code
     * @param toCurrency   Target currency code
     * @return Converted amount
     */
    BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency);

    /**
     * Get exchange rate between two currencies
     * 
     * @param fromCurrency Source currency
     * @param toCurrency   Target currency
     * @return Exchange rate (multiplier)
     */
    BigDecimal getRate(String fromCurrency, String toCurrency);
}
