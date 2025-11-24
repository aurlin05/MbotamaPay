package com.mbotamapay.backend.integrations.feexpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * FeexPay payment initialization request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeexPayInitRequest {

    private BigDecimal amount;

    private String currency;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    private String email;

    private String description;

    @JsonProperty("callback_url")
    private String callbackUrl;

    @JsonProperty("return_url")
    private String returnUrl;

    @JsonProperty("order_id")
    private String orderId; // Our internal reference

    @JsonProperty("paymentMethod")
    private String paymentMethod; // "MOBILE", "CARD", or "ALL"
}
