package com.mbotamapay.backend.entity;

public enum TransactionType {
    // Existing types (keep for backward compatibility)
    P2P_TRANSFER,
    TOP_UP,
    PAYMENT_REQUEST,
    REFERRAL_BONUS,
    CASHBACK,
    TRANSACTION_FEE,

    // New types for complete platform support
    WITHDRAW, // Payout to external provider
    P2P_SEND, // P2P transfer (sender side)
    P2P_RECEIVE, // P2P transfer (receiver side)
    FEE, // Transaction fee
    REBALANCE, // Operator account rebalancing
    REFUND // Refund transaction
}
