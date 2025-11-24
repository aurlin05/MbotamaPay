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
public class CinetPayLoginResponse {
    private Integer code;
    private String message;
    private CinetPayTokenData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CinetPayTokenData {
        private String token;
    }
}
