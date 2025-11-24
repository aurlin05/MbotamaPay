package com.mbotamapay.backend.integrations.feexpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FeexPay payment initialization response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeexPayInitResponse {

    private String reference;

    @JsonProperty("payment_url")
    private String paymentUrl;

    private String status; // PENDING, SUCCESS, FAILED

    private String message;

    @JsonProperty("order_id")
    private String orderId;
}
