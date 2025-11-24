package com.mbotamapay.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbotamapay.backend.dto.AuditLogQuery;
import com.mbotamapay.backend.entity.*;
import com.mbotamapay.backend.repository.AuditLogRepository;
import com.mbotamapay.backend.service.AuditService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void logTransaction(Transaction transaction, User initiator) {
        Map<String, Object> details = new HashMap<>();
        details.put("transactionId", transaction.getId());
        details.put("reference", transaction.getReference());
        details.put("amount", transaction.getAmount());
        details.put("fee", transaction.getFee());
        details.put("type", transaction.getType());
        details.put("status", transaction.getStatus());
        details.put("description", transaction.getDescription());
        
        if (transaction.getSenderWallet() != null) {
            details.put("senderWalletId", transaction.getSenderWallet().getId());
        }
        if (transaction.getReceiverWallet() != null) {
            details.put("receiverWalletId", transaction.getReceiverWallet().getId());
        }

        AuditLog auditLog = AuditLog.builder()
                .user(initiator)
                .username(initiator != null ? initiator.getEmail() : "SYSTEM")
                .actionType("TRANSACTION_" + transaction.getType())
                .action("TRANSACTION_" + transaction.getType())
                .severity(AuditSeverity.INFO)
                .entityId(transaction.getId() != null ? transaction.getId().toString() : null)
                .entityType("Transaction")
                .ipAddress(getClientIpAddress())
                .details(toJson(details))
                .createdAt(LocalDateTime.now())
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Transaction audit log created for transaction: {}", transaction.getReference());
    }

    @Override
    @Async
    public void logWalletModification(Wallet wallet, BigDecimal oldBalance, BigDecimal newBalance, User initiator) {
        Map<String, Object> details = new HashMap<>();
        details.put("walletId", wallet.getId());
        details.put("userId", wallet.getUser().getId());
        details.put("oldBalance", oldBalance);
        details.put("newBalance", newBalance);
        details.put("difference", newBalance.subtract(oldBalance));
        details.put("currency", wallet.getCurrency());

        AuditLog auditLog = AuditLog.builder()
                .user(initiator)
                .username(initiator != null ? initiator.getEmail() : "SYSTEM")
                .actionType("WALLET_MODIFICATION")
                .action("WALLET_MODIFICATION")
                .severity(AuditSeverity.INFO)
                .entityId(wallet.getId().toString())
                .entityType("Wallet")
                .ipAddress(getClientIpAddress())
                .details(toJson(details))
                .createdAt(LocalDateTime.now())
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Wallet modification audit log created for wallet: {}", wallet.getId());
    }

    @Override
    @Async
    public void logAdminAction(String action, User admin, Map<String, Object> details) {
        AuditLog auditLog = AuditLog.builder()
                .user(admin)
                .username(admin != null ? admin.getEmail() : "SYSTEM")
                .actionType("ADMIN_" + action)
                .action("ADMIN_" + action)
                .severity(AuditSeverity.WARNING)
                .entityId(null)
                .entityType("Admin")
                .ipAddress(getClientIpAddress())
                .details(toJson(details))
                .createdAt(LocalDateTime.now())
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Admin action audit log created: {} by {}", action, admin != null ? admin.getEmail() : "SYSTEM");
    }

    @Override
    @Async
    public void logSecurityEvent(String eventType, String severity, Map<String, Object> details) {
        User currentUser = getCurrentUser();
        
        AuditSeverity auditSeverity;
        try {
            auditSeverity = AuditSeverity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            auditSeverity = AuditSeverity.WARNING;
        }

        AuditLog auditLog = AuditLog.builder()
                .user(currentUser)
                .username(currentUser != null ? currentUser.getEmail() : "ANONYMOUS")
                .actionType("SECURITY_" + eventType)
                .action("SECURITY_" + eventType)
                .severity(auditSeverity)
                .entityId(null)
                .entityType("Security")
                .ipAddress(getClientIpAddress())
                .details(toJson(details))
                .createdAt(LocalDateTime.now())
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.warn("Security event audit log created: {} with severity {}", eventType, severity);
    }

    @Override
    public Page<AuditLog> queryAuditLogs(AuditLogQuery query, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditLog> cq = cb.createQuery(AuditLog.class);
        Root<AuditLog> root = cq.from(AuditLog.class);

        List<Predicate> predicates = new ArrayList<>();

        if (query.getUserId() != null) {
            predicates.add(cb.equal(root.get("user").get("id"), query.getUserId()));
        }

        if (query.getActionType() != null && !query.getActionType().isEmpty()) {
            predicates.add(cb.equal(root.get("actionType"), query.getActionType()));
        }

        if (query.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), query.getStartDate()));
        }

        if (query.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), query.getEndDate()));
        }

        if (query.getSeverity() != null && !query.getSeverity().isEmpty()) {
            try {
                AuditSeverity severity = AuditSeverity.valueOf(query.getSeverity().toUpperCase());
                predicates.add(cb.equal(root.get("severity"), severity));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid severity value: {}", query.getSeverity());
            }
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("createdAt")));

        List<AuditLog> results = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<AuditLog> countRoot = countQuery.from(AuditLog.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    @Async
    public void logAction(String username, String action, String entityId, String entityType, String details, String ipAddress) {
        AuditLog log = AuditLog.builder()
                .username(username)
                .action(action)
                .actionType(action)
                .severity(AuditSeverity.INFO)
                .entityId(entityId)
                .entityType(entityType)
                .details(details)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    @Override
    @Async
    public void logAction(String username, String action, String details, String ipAddress) {
        logAction(username, action, null, null, details, ipAddress);
    }

    /**
     * Get the current authenticated user from SecurityContext
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                org.springframework.security.core.userdetails.UserDetails userDetails = 
                    (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
                
                // If using CustomUserDetails, we can get the User object directly
                if (userDetails instanceof com.mbotamapay.backend.security.CustomUserDetails) {
                    return ((com.mbotamapay.backend.security.CustomUserDetails) userDetails).getUser();
                }
            }
        } catch (Exception e) {
            log.debug("Could not retrieve current user from SecurityContext", e);
        }
        return null;
    }

    /**
     * Get the client IP address from the current HTTP request
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Check for X-Forwarded-For header (proxy/load balancer)
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                // Check for X-Real-IP header
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                
                // Fall back to remote address
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not retrieve client IP address", e);
        }
        return "UNKNOWN";
    }

    /**
     * Convert a map to JSON string
     */
    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert data to JSON", e);
            return data.toString();
        }
    }
}
