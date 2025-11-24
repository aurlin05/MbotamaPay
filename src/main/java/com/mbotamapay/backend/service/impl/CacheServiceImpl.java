package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Implementation of CacheService for managing cache eviction.
 * Uses Spring's CacheManager to evict cache entries by name and key.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheServiceImpl implements CacheService {
    
    private final CacheManager cacheManager;
    
    @Override
    public void evictWalletCache(Long walletId) {
        if (walletId == null) {
            log.warn("Attempted to evict wallet cache with null walletId");
            return;
        }
        
        evictCache("wallets", walletId);
        evictCache("walletBalance", walletId);
        log.debug("Evicted wallet cache for walletId: {}", walletId);
    }
    
    @Override
    public void evictUserCache(Long userId) {
        if (userId == null) {
            log.warn("Attempted to evict user cache with null userId");
            return;
        }
        
        evictCache("users", userId);
        log.debug("Evicted user cache for userId: {}", userId);
    }
    
    @Override
    public void evictTransactionHistoryCache(Long userId) {
        if (userId == null) {
            log.warn("Attempted to evict transaction history cache with null userId");
            return;
        }
        
        // Evict all entries in the transaction history cache for this user
        // Since transaction history may be paginated, we evict the entire cache
        Cache cache = cacheManager.getCache("transactionHistory");
        if (cache != null) {
            cache.clear();
            log.debug("Cleared transaction history cache (affected userId: {})", userId);
        }
    }
    
    /**
     * Helper method to evict a specific cache entry.
     * 
     * @param cacheName the name of the cache
     * @param key the cache key to evict
     */
    private void evictCache(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        } else {
            log.warn("Cache '{}' not found in CacheManager", cacheName);
        }
    }
}
