package com.mbotamapay.backend.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInitResponse {
    private String paymentUrl;
    private String transactionId;
    private String paymentToken;
}
