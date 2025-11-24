package com.mbotamapay.backend.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestResponse {
    private Long id;
    private String requesterName;
    private String requesterEmail;
    private String payerName;
    private String payerEmail;
    private BigDecimal amount;
    private String status;
    private String description;
    private LocalDateTime createdAt;
}
