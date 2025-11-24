package com.mbotamapay.backend.integrations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Common payment verification response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerifyResponse {
    private String reference;
    private String status; // PENDING, SUCCESS, FAILED
    private BigDecimal amount;
    private String currency;
    private String phoneNumber;
    private String message;
    private String reason; // Failure reason if applicable
}
