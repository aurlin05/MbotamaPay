package com.mbotamapay.backend.integrations.feexpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * FeexPay payout status response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeexPayPayoutStatusResponse {

    private String reference;

    private String status; // SUCCESSFUL, FAILED, PENDING

    private BigDecimal amount;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    private String message;
}
