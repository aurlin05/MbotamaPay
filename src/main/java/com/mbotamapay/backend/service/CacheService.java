package com.mbotamapay.backend.service;

/**
 * Service for managing cache eviction across the application.
 * Provides methods to invalidate cache entries when data is modified.
 */
public interface CacheService {
    
    /**
     * Evict wallet cache for a specific wallet ID.
     * Should be called after any wallet balance modification.
     * 
     * @param walletId the ID of the wallet whose cache should be evicted
     */
    void evictWalletCache(Long walletId);
    
    /**
     * Evict user cache for a specific user ID.
     * Should be called after any user data modification.
     * 
     * @param userId the ID of the user whose cache should be evicted
     */
    void evictUserCache(Long userId);
    
    /**
     * Evict transaction history cache for a specific user ID.
     * Should be called after any transaction involving the user.
     * 
     * @param userId the ID of the user whose transaction history cache should be evicted
     */
    void evictTransactionHistoryCache(Long userId);
}
