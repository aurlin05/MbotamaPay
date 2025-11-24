package com.mbotamapay.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * WebhookLog entity for auditing webhook events from payment providers.
 * Stores raw payloads and signatures for security and debugging.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "webhook_logs")
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    @NotNull
    private String provider; // FEEXPAY or CINETPAY

    @Column(name = "event_type", nullable = false, length = 100)
    @NotNull
    private String eventType; // e.g., PAYMENT_SUCCESS, PAYOUT_COMPLETED

    /**
     * Raw JSON payload from provider
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    @NotNull
    private String payload;

    /**
     * HMAC signature for verification
     */
    @Column(length = 500)
    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private WebhookStatus status = WebhookStatus.RECEIVED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum WebhookStatus {
        RECEIVED, // Webhook received but not yet processed
        PROCESSED, // Successfully processed
        FAILED // Processing failed
    }

    /**
     * Mark webhook as processed
     */
    public void markAsProcessed() {
        this.status = WebhookStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Mark webhook as failed with error message
     */
    public void markAsFailed(String errorMessage) {
        this.status = WebhookStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }
}
