package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.WebhookLog;
import com.mbotamapay.backend.entity.WebhookLog.WebhookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {

    /**
     * Find all webhook logs for a specific provider
     */
    Page<WebhookLog> findByProviderOrderByCreatedAtDesc(String provider, Pageable pageable);

    /**
     * Find all webhook logs by status
     */
    Page<WebhookLog> findByStatusOrderByCreatedAtDesc(WebhookStatus status, Pageable pageable);

    /**
     * Find all webhook logs for a provider and status
     */
    Page<WebhookLog> findByProviderAndStatusOrderByCreatedAtDesc(
            String provider,
            WebhookStatus status,
            Pageable pageable);

    /**
     * Find failed webhook logs for retry
     */
    List<WebhookLog> findByStatusAndCreatedAtAfter(WebhookStatus status, LocalDateTime after);

    /**
     * Count webhooks by provider and status
     */
    @Query("SELECT COUNT(w) FROM WebhookLog w WHERE w.provider = :provider AND w.status = :status")
    long countByProviderAndStatus(@Param("provider") String provider, @Param("status") WebhookStatus status);

    /**
     * Find recent webhook logs (for monitoring)
     */
    List<WebhookLog> findTop100ByOrderByCreatedAtDesc();
}
