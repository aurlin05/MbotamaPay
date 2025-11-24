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
 * OperatorAccount entity for tracking merchant balances on payment providers.
 * Used for liquidity management and bridge operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "operator_accounts", uniqueConstraints = @UniqueConstraint(name = "unique_provider_currency", columnNames = {
        "provider", "currency" }))
public class OperatorAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version; // For optimistic locking

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull
    private Provider provider;

    @Column(nullable = false, length = 10)
    @NotNull
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    @PositiveOrZero
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "reserved_balance", nullable = false, precision = 19, scale = 2)
    @PositiveOrZero
    @Builder.Default
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Get available balance (balance - reserved)
     */
    public BigDecimal getAvailableBalance() {
        return balance.subtract(reservedBalance);
    }

    /**
     * Check if sufficient balance is available for a payout
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return getAvailableBalance().compareTo(amount) >= 0;
    }

    public enum Provider {
        FEEXPAY,
        CINETPAY
    }
}
