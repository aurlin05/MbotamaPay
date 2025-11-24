package com.mbotamapay.backend.dto.qr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QRCodeResponse {
    private String qrCodeData;
    private String qrCodeBase64;
}
