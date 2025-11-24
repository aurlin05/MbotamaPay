package com.mbotamapay.backend.dto.user;

import com.mbotamapay.backend.validation.ValidEmail;
import com.mbotamapay.backend.validation.ValidPhoneNumber;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @ValidEmail
    private String email;

    @NotBlank(message = "Phone is required")
    @ValidPhoneNumber
    private String phone;
}
