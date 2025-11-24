package com.mbotamapay.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ExchangeRate entity for currency conversion rates.
 * Supports multi-currency wallet operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exchange_rates", uniqueConstraints = @UniqueConstraint(name = "unique_currency_pair", columnNames = {
        "from_currency", "to_currency" }))
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_currency", nullable = false, length = 10)
    @NotNull
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 10)
    @NotNull
    private String toCurrency;

    @Column(nullable = false, precision = 19, scale = 6)
    @Positive
    @NotNull
    private BigDecimal rate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Convert amount from source currency to target currency
     */
    public BigDecimal convert(BigDecimal amount) {
        return amount.multiply(rate);
    }
}
