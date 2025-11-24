package com.mbotamapay.backend.dto.transaction;

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
public class TransactionResponse {
    private Long id;
    private String reference;
    private String type;
    private String status;
    private BigDecimal amount;
    private String senderEmail;
    private String receiverEmail;
    private String description;
    private LocalDateTime createdAt;
}
