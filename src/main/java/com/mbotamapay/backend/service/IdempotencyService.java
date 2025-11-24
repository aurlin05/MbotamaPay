package com.mbotamapay.backend.service;

import com.mbotamapay.backend.dto.transaction.TransactionResponse;

import java.util.Optional;

/**
 * Service for managing idempotency of transaction operations.
 * Ensures that duplicate requests with the same idempotency key are processed only once.
 */
public interface IdempotencyService {
    
    /**
     * Check if an idempotency key has already been processed.
     * 
     * @param idempotencyKey the unique idempotency key
     * @return Optional containing the transaction response if already processed, empty otherwise
     */
    Optional<TransactionResponse> checkIdempotency(String idempotencyKey);
    
    /**
     * Store the result of a transaction with its idempotency key.
     * 
     * @param idempotencyKey the unique idempotency key
     * @param result the transaction response to store
     */
    void storeIdempotencyResult(String idempotencyKey, TransactionResponse result);
    
    /**
     * Check if a transaction with the given idempotency key is currently being processed.
     * 
     * @param idempotencyKey the unique idempotency key
     * @return true if the transaction is currently being processed, false otherwise
     */
    boolean isProcessing(String idempotencyKey);
    
    /**
     * Mark a transaction as currently being processed.
     * 
     * @param idempotencyKey the unique idempotency key
     */
    void markAsProcessing(String idempotencyKey);
}
