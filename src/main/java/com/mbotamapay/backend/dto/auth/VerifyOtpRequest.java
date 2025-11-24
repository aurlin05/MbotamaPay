package com.mbotamapay.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifyOtpRequest {

    @NotBlank(message = "Recipient is required")
    private String recipient;

    @NotBlank(message = "OTP code is required")
    private String code;
}
