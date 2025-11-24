package com.mbotamapay.backend.integrations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Common payment initialization request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitRequest {
    private BigDecimal amount;
    private String currency;
    private String phoneNumber;
    private String email;
    private String description;
    private String callbackUrl;
    private String returnUrl;
    private String referenceId; // Our internal reference
}
