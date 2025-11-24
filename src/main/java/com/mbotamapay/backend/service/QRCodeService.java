package com.mbotamapay.backend.service;

import com.mbotamapay.backend.dto.qr.QRCodeGenerateRequest;
import com.mbotamapay.backend.dto.qr.QRCodePaymentData;
import com.mbotamapay.backend.dto.qr.QRCodeResponse;
import com.mbotamapay.backend.entity.User;

public interface QRCodeService {
    QRCodeResponse generateQRCode(User user, QRCodeGenerateRequest request);

    QRCodePaymentData decodeQRCode(String qrData);
}
