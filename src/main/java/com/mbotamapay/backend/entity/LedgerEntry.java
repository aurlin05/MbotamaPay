package com.mbotamapay.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * LedgerEntry entity for double-entry accounting.
 * Every financial transaction creates two entries: one DEBIT and one CREDIT.
 * This ensures the accounting equation always balances.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference ID linking related ledger entries (e.g., same transaction creates 2
     * entries)
     */
    @Column(name = "reference_id", nullable = false)
    @NotNull
    private String referenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @NotNull
    private Wallet wallet;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @NotNull
    private String currency;

    /**
     * DEBIT (money out) or CREDIT (money in)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    @NotNull
    private EntryType entryType;

    /**
     * Type of transaction that created this entry
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    @NotNull
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull
    @Builder.Default
    private LedgerStatus status = LedgerStatus.SUCCESS;

    /**
     * Additional metadata stored as JSON string
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum EntryType {
        DEBIT, // Money out (decrease balance)
        CREDIT // Money in (increase balance)
    }

    public enum LedgerStatus {
        PENDING,
        SUCCESS,
        FAILED,
        REVERSED
    }
}
