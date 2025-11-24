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
public class CinetPayTransferRequest {
    @JsonProperty("prefix")
    private String prefix; // Country prefix e.g., "229"

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("client_transaction_id")
    private String clientTransactionId;

    @JsonProperty("notify_url")
    private String notifyUrl;

    @JsonProperty("token")
    private String token; // Auth token

    // Optional fields depending on CinetPay API version
    @JsonProperty("lang")
    @Builder.Default
    private String lang = "fr";
}
