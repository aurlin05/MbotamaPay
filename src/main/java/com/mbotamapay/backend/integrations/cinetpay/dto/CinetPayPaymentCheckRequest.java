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
public class CinetPayPaymentCheckRequest {
    @JsonProperty("apikey")
    private String apiKey;

    @JsonProperty("site_id")
    private String siteId;

    @JsonProperty("transaction_id")
    private String transactionId;
}
