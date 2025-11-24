package com.mbotamapay.backend.integrations.feexpay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbotamapay.backend.integrations.PaymentProvider;
import com.mbotamapay.backend.integrations.dto.*;
import com.mbotamapay.backend.integrations.feexpay.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeexPayProvider implements PaymentProvider {

    private final FeexPayClient feexPayClient;
    private final ObjectMapper objectMapper;

    @Value("${feexpay.webhook-secret}")
    private String webhookSecret;

    private static final List<String> SUPPORTED_COUNTRIES = Arrays.asList("BJ", "TG", "CG", "CI");

    @Override
    public String getProviderName() {
        return "FEEXPAY";
    }

    @Override
    public PaymentInitResponse initiatePayment(PaymentInitRequest request) {
        log.info("Initiating FeexPay payment for reference: {}", request.getReferenceId());

        FeexPayInitRequest feexRequest = FeexPayInitRequest.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .description(request.getDescription())
                .callbackUrl(request.getCallbackUrl())
                .returnUrl(request.getReturnUrl())
                .orderId(request.getReferenceId())
                .paymentMethod("MOBILE") // Default to MOBILE for P2P context
                .build();

        try {
            FeexPayInitResponse response = feexPayClient.initiatePayment(feexRequest);

            return PaymentInitResponse.builder()
                    .reference(response.getReference())
                    .paymentUrl(response.getPaymentUrl())
                    .status(response.getStatus())
                    .message(response.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error initiating FeexPay payment", e);
            throw new RuntimeException("Failed to initiate payment with FeexPay", e);
        }
    }

    @Override
    public PaymentVerifyResponse verifyPayment(String reference) {
        log.info("Verifying FeexPay payment status for reference: {}", reference);

        try {
            FeexPayStatusResponse response = feexPayClient.getPaymentStatus(reference);

            return PaymentVerifyResponse.builder()
                    .reference(response.getReference())
                    .status(mapStatus(response.getStatus()))
                    .amount(response.getAmount())
                    .phoneNumber(response.getPhoneNumber())
                    .message("Payment verification successful")
                    .reason(response.getReason())
                    .build();
        } catch (Exception e) {
            log.error("Error verifying FeexPay payment", e);
            throw new RuntimeException("Failed to verify payment with FeexPay", e);
        }
    }

    @Override
    public PayoutResponse initiatePayout(PayoutRequest request) {
        log.info("Initiating FeexPay payout for reference: {}", request.getReferenceId());

        FeexPayPayoutRequest feexRequest = FeexPayPayoutRequest.builder()
                .amount(request.getAmount())
                .phoneNumber(request.getPhoneNumber())
                .description(request.getDescription())
                .orderId(request.getReferenceId())
                // Country could be inferred or passed, for now we assume it's handled by
                // FeexPay global endpoint
                .build();

        try {
            FeexPayPayoutResponse response = feexPayClient.initiatePayout(feexRequest);

            return PayoutResponse.builder()
                    .reference(response.getReference())
                    .status(mapStatus(response.getStatus()))
                    .message(response.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error initiating FeexPay payout", e);
            throw new RuntimeException("Failed to initiate payout with FeexPay", e);
        }
    }

    @Override
    public PayoutStatusResponse checkPayoutStatus(String reference) {
        log.info("Checking FeexPay payout status for reference: {}", reference);

        try {
            FeexPayPayoutStatusResponse response = feexPayClient.getPayoutStatus(reference);

            return PayoutStatusResponse.builder()
                    .reference(response.getReference())
                    .status(mapStatus(response.getStatus()))
                    .amount(response.getAmount())
                    .phoneNumber(response.getPhoneNumber())
                    .message(response.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error checking FeexPay payout status", e);
            throw new RuntimeException("Failed to check payout status with FeexPay", e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (signature == null || webhookSecret == null) {
            return false;
        }
        // FeexPay uses HMAC SHA256
        String calculatedSignature = new HmacUtils("HmacSHA256", webhookSecret).hmacHex(payload);
        return calculatedSignature.equals(signature);
    }

    @Override
    public boolean supportsCountry(String countryCode) {
        return SUPPORTED_COUNTRIES.contains(countryCode);
    }

    private String mapStatus(String providerStatus) {
        if (providerStatus == null)
            return "PENDING";

        switch (providerStatus.toUpperCase()) {
            case "SUCCESSFUL":
            case "SUCCESS":
                return "SUCCESS";
            case "FAILED":
                return "FAILED";
            case "PENDING":
                return "PENDING";
            default:
                return providerStatus;
        }
    }
}
