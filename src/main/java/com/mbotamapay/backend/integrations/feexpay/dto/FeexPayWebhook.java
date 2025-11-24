package com.mbotamapay.backend.integrations.feexpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * FeexPay Webhook payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeexPayWebhook {

    private String reference;

    @JsonProperty("order_id")
    private String orderId;

    private String status; // SUCCESSFUL, FAILED

    private BigDecimal amount;

    @JsonProperty("callback_info")
    private String callbackInfo;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String email;

    private String type; // Paiement

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    private String date;

    private String reseau; // MTN, MOOV, etc.

    private String description;

    private String reason; // Failure reason
}
