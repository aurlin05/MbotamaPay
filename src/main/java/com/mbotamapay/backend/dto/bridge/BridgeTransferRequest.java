package com.mbotamapay.backend.dto.bridge;

import com.mbotamapay.backend.entity.OperatorAccount;
import jakarta.validation.constraints.DecimalMin;
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
public class BridgeTransferRequest {
    @NotNull(message = "From provider is required")
    private OperatorAccount.Provider fromProvider;

    @NotNull(message = "To provider is required")
    private OperatorAccount.Provider toProvider;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Recipient phone is required")
    private String recipientPhone;

    @NotNull(message = "Wallet ID is required")
    private Long walletId;

    // Optional metadata
    private String description;
    private String clientReferenceId;
}
