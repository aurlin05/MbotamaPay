package com.mbotamapay.backend.integrations.feexpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FeexPay payout response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeexPayPayoutResponse {

    private String reference;

    private String status; // PENDING, SUCCESS, FAILED

    private String message;
}
