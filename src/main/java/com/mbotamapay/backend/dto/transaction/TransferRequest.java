
package com.mbotamapay.backend.dto.transaction;

import com.mbotamapay.backend.validation.ValidTransactionAmount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {

    @NotBlank(message = "Recipient identifier is required")
    private String recipientIdentifier; // Email or phone

    @NotNull(message = "Amount is required")
    @ValidTransactionAmount
    private BigDecimal amount;

    private String description;

    private String otp; // Optional, required if MFA enabled

    private String idempotencyKey; // Optional, for idempotent transaction processing
}
