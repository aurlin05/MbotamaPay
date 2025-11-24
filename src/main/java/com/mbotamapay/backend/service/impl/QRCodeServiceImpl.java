package com.mbotamapay.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mbotamapay.backend.dto.qr.QRCodeGenerateRequest;
import com.mbotamapay.backend.dto.qr.QRCodePaymentData;
import com.mbotamapay.backend.dto.qr.QRCodeResponse;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.service.QRCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeServiceImpl implements QRCodeService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public QRCodeResponse generateQRCode(User user, QRCodeGenerateRequest request) {
        log.info("Generating QR code for user: {}", user.getEmail());

        try {
            // Create payment data
            QRCodePaymentData paymentData = QRCodePaymentData.builder()
                    .userId(user.getId().toString())
                    .email(user.getEmail())
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .build();

            String qrData = objectMapper.writeValueAsString(paymentData);

            // Generate QR Code
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 300, 300, hints);

            // Convert to image
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

            return QRCodeResponse.builder()
                    .qrCodeData(qrData)
                    .qrCodeBase64("data:image/png;base64," + base64Image)
                    .build();

        } catch (WriterException | IOException e) {
            log.error("Error generating QR code", e);
            throw new BusinessException("Failed to generate QR code");
        }
    }

    @Override
    public QRCodePaymentData decodeQRCode(String qrData) {
        log.info("Decoding QR code data");
        try {
            return objectMapper.readValue(qrData, QRCodePaymentData.class);
        } catch (Exception e) {
            log.error("Error decoding QR code", e);
            throw new BusinessException("Invalid QR code data");
        }
    }
}
