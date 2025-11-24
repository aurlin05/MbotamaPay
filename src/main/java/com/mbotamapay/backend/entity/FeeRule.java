package com.mbotamapay.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FeeRule entity for configurable transaction fees.
 * Fees can be configured per provider and transaction type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fee_rules")
public class FeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    @NotNull
    private String provider; // FEEXPAY, CINETPAY, or INTERNAL

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    @NotNull
    private TransactionType transactionType;

    /**
     * Percentage fee (e.g., 1.50 means 1.5%)
     */
    @Column(name = "percentage_fee", nullable = false, precision = 5, scale = 2)
    @PositiveOrZero
    @Builder.Default
    private BigDecimal percentageFee = BigDecimal.ZERO;

    /**
     * Fixed fee in minor currency units
     */
    @Column(name = "fixed_fee", nullable = false, precision = 19, scale = 2)
    @PositiveOrZero
    @Builder.Default
    private BigDecimal fixedFee = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    @NotNull
    private String currency;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Calculate fee for given amount
     * Formula: (amount * percentageFee / 100) + fixedFee
     */
    public BigDecimal calculateFee(BigDecimal amount) {
        BigDecimal percentagePart = amount.multiply(percentageFee).divide(new BigDecimal("100"));
        return percentagePart.add(fixedFee);
    }
}
