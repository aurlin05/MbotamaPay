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
public class CinetPayPaymentInitRequest {
    @JsonProperty("apikey")
    private String apiKey;

    @JsonProperty("site_id")
    private String siteId;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("description")
    private String description;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_surname")
    private String customerSurname;

    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("customer_phone_number")
    private String customerPhoneNumber;

    @JsonProperty("notify_url")
    private String notifyUrl;

    @JsonProperty("return_url")
    private String returnUrl;

    @JsonProperty("channels")
    private String channels;

    @JsonProperty("metadata")
    private String metadata;
}
