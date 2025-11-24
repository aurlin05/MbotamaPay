package com.mbotamapay.backend.integrations.cinetpay;

import com.mbotamapay.backend.integrations.cinetpay.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for CinetPay API
 */
@FeignClient(name = "cinetpay-client", url = "${cinetpay.base-url}", configuration = CinetPayClientConfig.class)
public interface CinetPayClient {

    /**
     * Generate authentication token
     * POST /v1/auth/login
     */
    @PostMapping("/v1/auth/login")
    CinetPayLoginResponse login(@RequestBody CinetPayLoginRequest request);

    /**
     * Initialize payment
     * POST /v1/payment/initialize
     * Note: CinetPay V2 uses different endpoint structure, checking docs...
     * Docs say: https://api-checkout.cinetpay.com/v2/payment
     * But for transfer it's: https://client.cinetpay.com/v1/transfer
     * We need to handle both.
     * 
     * Let's assume this client is for the TRANSFER API (client.cinetpay.com)
     * and PAYMENT API (api-checkout.cinetpay.com) might need a different client or
     * config.
     * 
     * Actually, for Payment Init, it's usually public API key based.
     * For Transfer, it's Token based.
     */

    /**
     * Send money (Transfer)
     * POST /v1/transfer/money/send/contact
     */
    @PostMapping("/v1/transfer/money/send/contact")
    CinetPayTransferResponse sendMoney(@RequestBody CinetPayTransferRequest request);

    /**
     * Check transfer status
     * POST /v1/transfer/check/money
     */
    @PostMapping("/v1/transfer/check/money")
    CinetPayTransferStatusResponse checkTransferStatus(@RequestBody CinetPayCheckTransferRequest request);

    // For Payment Initialization (Collection), CinetPay uses a different base URL
    // usually.
    // We might need a separate client or use full URL.
    // Let's use a separate method with URI parameter or just a different client.
    // For simplicity, I'll put payment init here but we might need to override URL.
    // Actually, let's keep it simple. The Provider will handle URL differences if
    // needed.
    // But FeignClient has fixed URL.

    // Let's create a separate client for Payment (Checkout) if needed,
    // but CinetPay documentation often mixes them.
    // The user provided docs for TRANSFER.
    // Existing code has Payment Init. Let's check existing CinetPayService.
}
