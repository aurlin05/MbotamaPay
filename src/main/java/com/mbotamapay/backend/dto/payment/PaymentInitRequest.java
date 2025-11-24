package com.mbotamapay.backend.dto.payment;

import jakarta.validation.constraints.DecimalMin;
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
public class PaymentInitRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100", message = "Minimum amount is 100")
    private BigDecimal amount;

    private String currency;
}
