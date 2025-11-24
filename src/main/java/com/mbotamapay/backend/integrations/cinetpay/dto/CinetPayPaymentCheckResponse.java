package com.mbotamapay.backend.integrations.cinetpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinetPayPaymentCheckResponse {
    private String code;
    private String message;
    private CinetPayPaymentCheckData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CinetPayPaymentCheckData {
        private BigDecimal amount;
        private String currency;
        private String status;
        private String operator_id;
        private String payment_method;
        private String description;
        private String metadata;
    }
}
