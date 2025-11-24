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
public class LoginRequest {

    @NotBlank(message = "Email or Phone is required")
    private String identifier; // Can be email or phone

    @NotBlank(message = "Password is required")
    private String password;
}
