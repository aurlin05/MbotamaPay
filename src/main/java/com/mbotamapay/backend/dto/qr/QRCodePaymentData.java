package com.mbotamapay.backend.dto.qr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QRCodePaymentData {
    private String userId;
    private String email;
    private BigDecimal amount;
    private String description;
}
