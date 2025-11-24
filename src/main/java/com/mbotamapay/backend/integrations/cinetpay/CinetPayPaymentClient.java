package com.mbotamapay.backend.integrations.cinetpay;

import com.mbotamapay.backend.integrations.cinetpay.dto.CinetPayPaymentInitRequest;
import com.mbotamapay.backend.integrations.cinetpay.dto.CinetPayPaymentInitResponse;
import com.mbotamapay.backend.integrations.cinetpay.dto.CinetPayPaymentCheckRequest;
import com.mbotamapay.backend.integrations.cinetpay.dto.CinetPayPaymentCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for CinetPay Payment (Checkout) API
 * URL: https://api-checkout.cinetpay.com/v2/payment
 */
@FeignClient(name = "cinetpay-payment-client", url = "https://api-checkout.cinetpay.com/v2/payment", configuration = CinetPayClientConfig.class)
public interface CinetPayPaymentClient {

    /**
     * Initialize payment
     * POST /
     */
    @PostMapping("")
    CinetPayPaymentInitResponse initializePayment(@RequestBody CinetPayPaymentInitRequest request);

    /**
     * Check payment status
     * POST /check
     */
    @PostMapping("/check")
    CinetPayPaymentCheckResponse checkPaymentStatus(@RequestBody CinetPayPaymentCheckRequest request);
}
