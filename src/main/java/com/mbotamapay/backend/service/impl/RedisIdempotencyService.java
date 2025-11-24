package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.idempotency.IdempotencyRecord;
import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.entity.IdempotencyStatus;
import com.mbotamapay.backend.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Redis-based implementation of IdempotencyService.
 * Uses Redis to store idempotency records with a 24-hour TTL.
 * Implements atomic operations using Redis transactions (MULTI/EXEC).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisIdempotencyService implements IdempotencyService {
    
    private static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "idempotency:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public Optional<TransactionResponse> checkIdempotency(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        
        String redisKey = KEY_PREFIX + idempotencyKey;
        
        try {
            Object value = redisTemplate.opsForValue().get(redisKey);
            
            if (value instanceof IdempotencyRecord record) {
                if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                    log.info("Found completed idempotency record for key: {}", idempotencyKey);
                    return Optional.ofNullable(record.getResult());
                }
            }
        } catch (Exception e) {
            log.error("Error checking idempotency for key: {}", idempotencyKey, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public void storeIdempotencyResult(String idempotencyKey, TransactionResponse result) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        
        String redisKey = KEY_PREFIX + idempotencyKey;
        
        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .key(idempotencyKey)
                    .result(result)
                    .status(IdempotencyStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plus(TTL))
                    .build();
            
            // Use Redis transaction for atomic operation
            redisTemplate.execute(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings({"rawtypes", "unchecked"})
                public Object execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    operations.opsForValue().set(redisKey, record);
                    operations.expire(redisKey, TTL);
                    return operations.exec();
                }
            });
            
            log.info("Stored idempotency result for key: {}", idempotencyKey);
        } catch (Exception e) {
            log.error("Error storing idempotency result for key: {}", idempotencyKey, e);
        }
    }
    
    @Override
    public boolean isProcessing(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        
        String redisKey = KEY_PREFIX + idempotencyKey;
        
        try {
            Object value = redisTemplate.opsForValue().get(redisKey);
            
            if (value instanceof IdempotencyRecord record) {
                return record.getStatus() == IdempotencyStatus.PROCESSING;
            }
        } catch (Exception e) {
            log.error("Error checking processing status for key: {}", idempotencyKey, e);
        }
        
        return false;
    }
    
    @Override
    public void markAsProcessing(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        
        String redisKey = KEY_PREFIX + idempotencyKey;
        
        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .key(idempotencyKey)
                    .status(IdempotencyStatus.PROCESSING)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plus(TTL))
                    .build();
            
            // Use setIfAbsent with TTL for atomic operation - only set if key doesn't exist
            // This is already atomic in Redis, no need for MULTI/EXEC
            Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(redisKey, record, TTL);
            
            if (Boolean.TRUE.equals(wasSet)) {
                log.info("Marked transaction as processing for key: {}", idempotencyKey);
            } else {
                log.warn("Transaction already exists for key: {}", idempotencyKey);
            }
        } catch (Exception e) {
            log.error("Error marking as processing for key: {}", idempotencyKey, e);
        }
    }
}
