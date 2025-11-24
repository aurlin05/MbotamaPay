package com.mbotamapay.backend.integrations.cinetpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinetPayPaymentInitResponse {
    private String code;
    private String message;
    private String description;
    private CinetPayPaymentData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CinetPayPaymentData {
        @JsonProperty("payment_url")
        private String paymentUrl;

        @JsonProperty("payment_token")
        private String paymentToken;
    }
}
