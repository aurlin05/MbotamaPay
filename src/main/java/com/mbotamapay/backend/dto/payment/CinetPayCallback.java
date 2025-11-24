package com.mbotamapay.backend.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CinetPayCallback {
    private String cpm_trans_id;
    private String cpm_site_id;
    private String cpm_trans_date;
    private BigDecimal cpm_amount;
    private String cpm_currency;
    private String signature;
    private String payment_method;
    private String cel_phone_num;
    private String cpm_phone_prefixe;
    private String cpm_language;
    private String cpm_version;
    private String cpm_payment_config;
    private String cpm_page_action;
    private String cpm_custom;
    private String cpm_designation;
    private String cpm_error_message;
}
