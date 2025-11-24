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
public class CinetPayCheckTransferRequest {
    @JsonProperty("client_transaction_id")
    private String clientTransactionId;

    @JsonProperty("token")
    private String token;
}
