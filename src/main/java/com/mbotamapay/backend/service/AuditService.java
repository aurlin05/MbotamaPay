package com.mbotamapay.backend.service;

import com.mbotamapay.backend.entity.Transaction;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Map;

public interface AuditService {

    /**
     * Log a transaction operation
     * @param transaction The transaction to log
     * @param initiator The user who initiated the transaction
     */
    void logTransaction(Transaction transaction, User initiator);

    /**
     * Log a wallet modification
     * @param wallet The wallet that was modified
     * @param oldBalance The balance before modification
     * @param newBalance The balance after modification
     * @param initiator The user who initiated the modification
     */
    void logWalletModification(Wallet wallet, BigDecimal oldBalance, BigDecimal newBalance, User initiator);

    /**
     * Log an admin action
     * @param action The action performed
     * @param admin The admin who performed the action
     * @param details Additional details about the action
     */
    void logAdminAction(String action, User admin, Map<String, Object> details);

    /**
     * Log a security event
     * @param eventType The type of security event
     * @param severity The severity level
     * @param details Additional details about the event
     */
    void logSecurityEvent(String eventType, String severity, Map<String, Object> details);

    /**
     * Query audit logs with filtering
     * @param query The query parameters
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    Page<com.mbotamapay.backend.entity.AuditLog> queryAuditLogs(com.mbotamapay.backend.dto.AuditLogQuery query, Pageable pageable);

    /**
     * Legacy method for backward compatibility
     */
    void logAction(String username, String action, String entityId, String entityType, String details, String ipAddress);

    /**
     * Legacy method for backward compatibility
     */
    void logAction(String username, String action, String details, String ipAddress);
}
