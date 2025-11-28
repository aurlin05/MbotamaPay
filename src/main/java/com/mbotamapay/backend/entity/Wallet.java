package com.mbotamapay.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    @PositiveOrZero(message = "Balance must be zero or positive")
    private BigDecimal balance;

    @Column(name = "reserved_balance", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    @Pattern(regexp = "XAF|EUR|USD|XOF|CDF", message = "Currency must be one of: XAF, EUR, USD, XOF, CDF")
    private String currency;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Get available balance (balance - reserved_balance)
     * Reserved balance is used for pending payouts/operations
     */
    public BigDecimal getAvailableBalance() {
        return balance.subtract(reservedBalance);
    }
}
