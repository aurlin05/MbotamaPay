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
public class CinetPayTransferStatusResponse {
    private Integer code;
    private String message;
    private CinetPayTransferStatusData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CinetPayTransferStatusData {
        @JsonProperty("transaction_id")
        private String transactionId;

        @JsonProperty("client_transaction_id")
        private String clientTransactionId;

        private String status;
        private BigDecimal amount;
        private String receiver;

        @JsonProperty("sending_status")
        private String sendingStatus;

        @JsonProperty("validated_at")
        private String validatedAt;
    }
}
