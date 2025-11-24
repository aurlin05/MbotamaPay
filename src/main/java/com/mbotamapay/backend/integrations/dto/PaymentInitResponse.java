package com.mbotamapay.backend.integrations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common payment initialization response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitResponse {
    private String reference; // Provider's reference
    private String paymentUrl; // URL to redirect user for payment
    private String status; // PENDING, SUCCESS, FAILED
    private String message;
}
