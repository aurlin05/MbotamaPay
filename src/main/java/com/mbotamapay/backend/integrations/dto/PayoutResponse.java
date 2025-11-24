package com.mbotamapay.backend.integrations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutResponse {
    private String reference;
    private String status; // SUCCESS, PENDING, FAILED
    private String message;
    private String providerReference; // Provider's transaction ID
}
