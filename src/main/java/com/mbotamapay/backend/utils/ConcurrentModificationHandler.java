package com.mbotamapay.backend.utils;

import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Handler for managing concurrent modification conflicts using optimistic locking.
 * Implements retry logic with exponential backoff for OptimisticLockException.
 */
@Component
@Slf4j
public class ConcurrentModificationHandler {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 100;

    /**
     * Executes an operation with retry logic for optimistic lock failures.
     * Uses exponential backoff between retries.
     *
     * @param operation The operation to execute
     * @param <T> The return type of the operation
     * @return The result of the operation
     * @throws OptimisticLockException if all retries are exhausted
     */
    public <T> T retryOnOptimisticLock(Supplier<T> operation) {
        return retryOnOptimisticLock(operation, MAX_RETRIES);
    }

    /**
     * Executes an operation with retry logic for optimistic lock failures.
     * Uses exponential backoff between retries.
     *
     * @param operation The operation to execute
     * @param maxRetries Maximum number of retry attempts
     * @param <T> The return type of the operation
     * @return The result of the operation
     * @throws OptimisticLockException if all retries are exhausted
     */
    public <T> T retryOnOptimisticLock(Supplier<T> operation, int maxRetries) {
        int attempt = 0;
        OptimisticLockException lastException = null;

        while (attempt < maxRetries) {
            try {
                return operation.get();
            } catch (OptimisticLockException e) {
                lastException = e;
                attempt++;
                
                if (attempt < maxRetries) {
                    long backoffTime = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
                    log.warn("Optimistic lock conflict detected. Retry attempt {} of {} after {}ms", 
                            attempt, maxRetries, backoffTime);
                    
                    try {
                        Thread.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    log.error("Optimistic lock conflict persisted after {} retries", maxRetries);
                }
            }
        }

        throw handleOptimisticLockException(lastException);
    }

    /**
     * Creates a clear error response for optimistic lock exceptions.
     *
     * @param ex The OptimisticLockException
     * @return A new OptimisticLockException with a clear message
     */
    public OptimisticLockException handleOptimisticLockException(OptimisticLockException ex) {
        String message = "The resource was modified by another transaction. Please retry your operation.";
        log.error("Optimistic lock exception: {}", message, ex);
        return new OptimisticLockException(message);
    }
}
