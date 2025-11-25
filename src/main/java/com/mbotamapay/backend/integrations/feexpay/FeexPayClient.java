package com.mbotamapay.backend.integrations.feexpay;

import com.mbotamapay.backend.integrations.feexpay.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for FeexPay API
 * Base URL configured via application properties: feexpay.base-url
 */
@FeignClient(name = "feexpay-client", url = "${app.feexpay.base-url}", configuration = FeexPayClientConfig.class)
public interface FeexPayClient {

    /**
     * Initialize a payment (topup/deposit)
     * POST /api/payment/init
     */
    @PostMapping("/api/payment/init")
    FeexPayInitResponse initiatePayment(@RequestBody FeexPayInitRequest request);

    /**
     * Check payment status
     * GET /api/payment/status/{reference}
     */
    @GetMapping("/api/payment/status/{reference}")
    FeexPayStatusResponse getPaymentStatus(@PathVariable("reference") String reference);

    /**
     * Initiate a payout (global endpoint for all providers)
     * POST /api/payouts/public/transfer/global
     */
    @PostMapping("/api/payouts/public/transfer/global")
    FeexPayPayoutResponse initiatePayout(@RequestBody FeexPayPayoutRequest request);

    /**
     * Check payout status
     * GET /api/payouts/status/public/{reference}
     */
    @GetMapping("/api/payouts/status/public/{reference}")
    FeexPayPayoutStatusResponse getPayoutStatus(@PathVariable("reference") String reference);
}
