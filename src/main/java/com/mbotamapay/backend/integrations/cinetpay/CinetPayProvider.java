package com.mbotamapay.backend.integrations.cinetpay;

import com.mbotamapay.backend.integrations.PaymentProvider;
import com.mbotamapay.backend.integrations.dto.*;
import com.mbotamapay.backend.integrations.cinetpay.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CinetPayProvider implements PaymentProvider {

    private final CinetPayClient cinetPayClient;
    private final CinetPayPaymentClient cinetPayPaymentClient;

    @Value("${cinetpay.api-key}")
    private String apiKey;

    @Value("${cinetpay.site-id}")
    private String siteId;

    @Value("${cinetpay.username:}")
    private String username;

    @Value("${cinetpay.password:}")
    private String password;

    @Value("${cinetpay.webhook-secret}")
    private String webhookSecret;

    @Value("${cinetpay.notify-url}")
    private String notifyUrl;

    @Value("${cinetpay.return-url}")
    private String returnUrl;

    private static final List<String> SUPPORTED_COUNTRIES = Arrays.asList("CI", "SN", "ML", "BF", "TG", "BJ", "NE",
            "GW", "CM", "CD");

    // Token management
    private String cachedToken;
    private LocalDateTime tokenExpiration;

    @Override
    public String getProviderName() {
        return "CINETPAY";
    }

    @Override
    public PaymentInitResponse initiatePayment(PaymentInitRequest request) {
        log.info("Initiating CinetPay payment for reference: {}", request.getReferenceId());

        CinetPayPaymentInitRequest cinetRequest = CinetPayPaymentInitRequest.builder()
                .apiKey(apiKey)
                .siteId(siteId)
                .transactionId(request.getReferenceId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .customerEmail(request.getEmail())
                .customerPhoneNumber(request.getPhoneNumber())
                .notifyUrl(notifyUrl)
                .returnUrl(returnUrl)
                .channels("ALL")
                .metadata(request.getReferenceId())
                .build();

        try {
            CinetPayPaymentInitResponse response = cinetPayPaymentClient.initializePayment(cinetRequest);

            if ("201".equals(response.getCode())) {
                return PaymentInitResponse.builder()
                        .reference(response.getData().getPaymentToken())
                        .paymentUrl(response.getData().getPaymentUrl())
                        .status("PENDING")
                        .message(response.getMessage())
                        .build();
            } else {
                throw new RuntimeException("CinetPay error: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("Error initiating CinetPay payment", e);
            throw new RuntimeException("Failed to initiate payment with CinetPay", e);
        }
    }

    @Override
    public PaymentVerifyResponse verifyPayment(String reference) {
        log.info("Verifying CinetPay payment status for reference: {}", reference);

        CinetPayPaymentCheckRequest checkRequest = CinetPayPaymentCheckRequest.builder()
                .apiKey(apiKey)
                .siteId(siteId)
                .transactionId(reference)
                .build();

        try {
            CinetPayPaymentCheckResponse response = cinetPayPaymentClient.checkPaymentStatus(checkRequest);

            if ("00".equals(response.getCode())) {
                return PaymentVerifyResponse.builder()
                        .reference(reference)
                        .status(mapPaymentStatus(response.getData().getStatus()))
                        .amount(response.getData().getAmount())
                        .currency(response.getData().getCurrency())
                        .message(response.getMessage())
                        .build();
            } else {
                return PaymentVerifyResponse.builder()
                        .reference(reference)
                        .status("FAILED")
                        .message(response.getMessage())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error verifying CinetPay payment", e);
            throw new RuntimeException("Failed to verify payment with CinetPay", e);
        }
    }

    @Override
    public PayoutResponse initiatePayout(PayoutRequest request) {
        log.info("Initiating CinetPay transfer for reference: {}", request.getReferenceId());

        ensureToken();

        CinetPayTransferRequest transferRequest = CinetPayTransferRequest.builder()
                .prefix("225") // Default to CI, logic should extract from phone
                .phoneNumber(request.getPhoneNumber()) // Should strip prefix if needed
                .amount(request.getAmount())
                .clientTransactionId(request.getReferenceId())
                .notifyUrl(notifyUrl)
                .token(cachedToken)
                .lang("fr")
                .build();

        // Extract prefix from phone number if possible
        if (request.getPhoneNumber() != null && request.getPhoneNumber().length() > 4) {
            // Simple heuristic, should be improved
            // Assuming format like 22507070707
            // CinetPay expects prefix separate
        }

        try {
            CinetPayTransferResponse response = cinetPayClient.sendMoney(transferRequest);

            if (response.getCode() == 0) {
                return PayoutResponse.builder()
                        .reference(response.getData().getTransactionId())
                        .status(mapTransferStatus(response.getData().getStatus()))
                        .message(response.getMessage())
                        .build();
            } else {
                throw new RuntimeException("CinetPay transfer error: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("Error initiating CinetPay transfer", e);
            throw new RuntimeException("Failed to initiate transfer with CinetPay", e);
        }
    }

    @Override
    public PayoutStatusResponse checkPayoutStatus(String reference) {
        log.info("Checking CinetPay transfer status for reference: {}", reference);

        ensureToken();

        CinetPayCheckTransferRequest checkRequest = CinetPayCheckTransferRequest.builder()
                .clientTransactionId(reference)
                .token(cachedToken)
                .build();

        try {
            CinetPayTransferStatusResponse response = cinetPayClient.checkTransferStatus(checkRequest);

            if (response.getCode() == 0) {
                return PayoutStatusResponse.builder()
                        .reference(response.getData().getTransactionId())
                        .status(mapTransferStatus(response.getData().getStatus()))
                        .amount(response.getData().getAmount())
                        .message(response.getMessage())
                        .build();
            } else {
                return PayoutStatusResponse.builder()
                        .reference(reference)
                        .status("FAILED")
                        .message(response.getMessage())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error checking CinetPay transfer status", e);
            throw new RuntimeException("Failed to check transfer status with CinetPay", e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        // CinetPay signature verification logic
        // Usually HMAC-SHA256 of payload or specific fields
        // For now, we'll assume standard HMAC check if payload is the data string
        // But CinetPay sends POST data.
        // We might need to reconstruct the string: cpm_trans_id + cpm_amount +
        // cpm_currency
        // Since we receive raw payload here, we might need to parse it or pass the
        // specific fields.
        // The interface takes raw payload.
        // Let's assume the controller extracts the necessary fields and reconstructs
        // the string to pass as 'payload' here
        // OR we parse it here.

        // For simplicity and robustness, let's assume 'payload' passed here is the data
        // string to sign
        // constructed by the controller/service.

        if (signature == null || webhookSecret == null) {
            return false;
        }
        String calculatedSignature = new HmacUtils("HmacSHA256", webhookSecret).hmacHex(payload);
        return calculatedSignature.equalsIgnoreCase(signature);
    }

    @Override
    public boolean supportsCountry(String countryCode) {
        return SUPPORTED_COUNTRIES.contains(countryCode);
    }

    private void ensureToken() {
        if (cachedToken == null || LocalDateTime.now().isAfter(tokenExpiration)) {
            log.info("Refreshing CinetPay auth token");
            CinetPayLoginRequest loginRequest = CinetPayLoginRequest.builder()
                    .apiKey(apiKey)
                    .password(password)
                    .build();

            try {
                CinetPayLoginResponse response = cinetPayClient.login(loginRequest);
                if (response.getCode() == 0) {
                    cachedToken = response.getData().getToken();
                    // Token validity isn't always returned, assume 1 hour safe
                    tokenExpiration = LocalDateTime.now().plusMinutes(50);
                } else {
                    throw new RuntimeException("Failed to login to CinetPay: " + response.getMessage());
                }
            } catch (Exception e) {
                log.error("Error logging in to CinetPay", e);
                throw new RuntimeException("CinetPay login failed", e);
            }
        }
    }

    private String mapPaymentStatus(String status) {
        if (status == null)
            return "PENDING";
        switch (status.toUpperCase()) {
            case "ACCEPTED":
                return "SUCCESS";
            case "REFUSED":
            case "CANCELLED":
                return "FAILED";
            default:
                return "PENDING";
        }
    }

    private String mapTransferStatus(String status) {
        if (status == null)
            return "PENDING";
        switch (status.toUpperCase()) {
            case "VALIDATED":
                return "SUCCESS";
            case "REJECTED":
                return "FAILED";
            default:
                return "PENDING";
        }
    }
}
