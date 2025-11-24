package com.mbotamapay.backend.integrations;

import com.mbotamapay.backend.integrations.dto.*;

/**
 * Common interface for all payment providers (FeexPay, CinetPay).
 * Provides abstraction for payment collection and payout operations.
 */
public interface PaymentProvider {

    /**
     * Get the provider name
     */
    String getProviderName();

    /**
     * Initiate a payment collection (deposit/topup)
     * 
     * @param request Payment initialization request
     * @return Payment initialization response with payment URL and reference
     */
    PaymentInitResponse initiatePayment(PaymentInitRequest request);

    /**
     * Verify payment status
     * 
     * @param reference Payment reference ID
     * @return Payment verification response with status
     */
    PaymentVerifyResponse verifyPayment(String reference);

    /**
     * Initiate a payout (withdrawal)
     * 
     * @param request Payout request
     * @return Payout response with reference and status
     */
    PayoutResponse initiatePayout(PayoutRequest request);

    /**
     * Check payout status
     * 
     * @param reference Payout reference ID
     * @return Payout status response
     */
    PayoutStatusResponse checkPayoutStatus(String reference);

    /**
     * Verify webhook signature using HMAC
     * 
     * @param payload   Raw webhook payload
     * @param signature Signature from webhook header
     * @return true if signature is valid
     */
    boolean verifyWebhookSignature(String payload, String signature);

    /**
     * Check if this provider supports the given country
     * 
     * @param countryCode ISO country code (e.g., "CI", "SN", "BJ")
     * @return true if provider supports this country
     */
    boolean supportsCountry(String countryCode);
}
