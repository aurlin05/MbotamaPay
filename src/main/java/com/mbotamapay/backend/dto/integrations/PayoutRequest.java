package com.mbotamapay.backend.dto.integrations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutRequest {
    @NotBlank(message = "Reference ID is required")
    private String referenceId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    private String description;
    private String network; // MTN, MOOV, ORANGE, etc.
}
