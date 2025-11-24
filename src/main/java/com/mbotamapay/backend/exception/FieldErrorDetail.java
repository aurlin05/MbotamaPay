package com.mbotamapay.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FieldErrorDetail {
    private String field;
    private String message;
    private Object rejectedValue;
}
