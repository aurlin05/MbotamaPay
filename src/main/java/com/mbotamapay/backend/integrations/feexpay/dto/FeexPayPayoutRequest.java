package com.mbotamapay.backend.integrations.feexpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * FeexPay payout request (global transfer)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeexPayPayoutRequest {

    private BigDecimal amount;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    private String description;

    @JsonProperty("callback_url")
    private String callbackUrl;

    @JsonProperty("order_id")
    private String orderId; // Our internal reference

    private String country; // BJ, TG, CI, etc. (Optional, inferred from phone usually but good to have)
}
