package com.mbotamapay.backend.integrations.feexpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * FeexPay payment status response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeexPayStatusResponse {

    private String reference;

    private String status; // SUCCESSFUL, FAILED, PENDING

    private BigDecimal amount;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    private String date;

    private String reseau; // MTN, MOOV, etc.

    @JsonProperty("order_id")
    private String orderId;

    private String reason; // Failure reason
}
