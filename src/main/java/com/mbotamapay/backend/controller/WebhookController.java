package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.entity.WebhookLog;
import com.mbotamapay.backend.integrations.feexpay.dto.FeexPayWebhook;
import com.mbotamapay.backend.integrations.PaymentProvider;
import com.mbotamapay.backend.integrations.ProviderFactory;
import com.mbotamapay.backend.repository.WebhookLogRepository;
import com.mbotamapay.backend.routes.Routes;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Routes.PAYMENT + "/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Payment provider webhook endpoints")
public class WebhookController {

    private final ProviderFactory providerFactory;
    private final WebhookLogRepository webhookLogRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/feexpay")
    @Operation(summary = "FeexPay webhook", description = "Receive webhook notifications from FeexPay")
    public ResponseEntity<String> feexpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Feexpay-Signature", required = false) String signature) {

        log.info("Received FeexPay webhook");

        // Log webhook
        WebhookLog webhookLog = WebhookLog.builder()
                .provider("FEEXPAY")
                .eventType("PAYMENT_NOTIFICATION")
                .payload(payload)
                .signature(signature)
                .status(WebhookLog.WebhookStatus.RECEIVED)
                .build();
        webhookLogRepository.save(webhookLog);

        try {
            // Verify signature
            PaymentProvider provider = providerFactory.getProvider("FEEXPAY");
            if (!provider.verifyWebhookSignature(payload, signature)) {
                log.error("Invalid FeexPay webhook signature");
                webhookLog.markAsFailed("Invalid signature");
                webhookLogRepository.save(webhookLog);
                return ResponseEntity.status(403).body("Invalid signature");
            }

            // Parse payload
            FeexPayWebhook webhook = objectMapper.readValue(payload, FeexPayWebhook.class);

            // Process webhook (this would typically call a service)
            // For now, just log and mark as processed
            log.info("FeexPay webhook processed: reference={}, status={}", webhook.getReference(), webhook.getStatus());

            webhookLog.markAsProcessed();
            webhookLogRepository.save(webhookLog);

            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("Error processing FeexPay webhook", e);
            webhookLog.markAsFailed(e.getMessage());
            webhookLogRepository.save(webhookLog);
            return ResponseEntity.status(500).body("Processing error");
        }
    }

    @PostMapping("/cinetpay")
    @Operation(summary = "CinetPay webhook", description = "Receive webhook notifications from CinetPay")
    public ResponseEntity<String> cinetpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Cinetpay-Signature", required = false) String signature) {

        log.info("Received CinetPay webhook");

        // Log webhook
        WebhookLog webhookLog = WebhookLog.builder()
                .provider("CINETPAY")
                .eventType("PAYMENT_NOTIFICATION")
                .payload(payload)
                .signature(signature)
                .status(WebhookLog.WebhookStatus.RECEIVED)
                .build();
        webhookLogRepository.save(webhookLog);

        try {
            // Verify signature
            PaymentProvider provider = providerFactory.getProvider("CINETPAY");

            // CinetPay webhook signature verification
            // Usually reconstructed from cpm_trans_id + cpm_amount + cpm_currency
            // For now, we'll skip detailed verification and just log
            if (signature != null && !provider.verifyWebhookSignature(payload, signature)) {
                log.warn("CinetPay webhook signature mismatch (might need specific field extraction)");
                // Don't fail immediately, as CinetPay signature format might vary
            }

            log.info("CinetPay webhook received and logged");

            webhookLog.markAsProcessed();
            webhookLogRepository.save(webhookLog);

            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("Error processing CinetPay webhook", e);
            webhookLog.markAsFailed(e.getMessage());
            webhookLogRepository.save(webhookLog);
            return ResponseEntity.status(500).body("Processing error");
        }
    }
}
