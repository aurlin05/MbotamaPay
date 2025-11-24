package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.dto.qr.QRCodeGenerateRequest;
import com.mbotamapay.backend.dto.qr.QRCodePaymentData;
import com.mbotamapay.backend.dto.qr.QRCodeResponse;
import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.dto.transaction.TransferRequest;
import com.mbotamapay.backend.security.CustomUserDetails;
import com.mbotamapay.backend.service.QRCodeService;
import com.mbotamapay.backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.mbotamapay.backend.routes.Routes;

@RestController
@RequestMapping(Routes.QR_CODE)
@RequiredArgsConstructor
@Tag(name = "QR Code", description = "QR Code payment APIs")
public class QRCodeController {

    private final QRCodeService qrCodeService;
    private final TransactionService transactionService;

    @PostMapping("/generate")
    @Operation(summary = "Generate QR code", description = "Generate a QR code for receiving payments")
    public ResponseEntity<QRCodeResponse> generateQRCode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody QRCodeGenerateRequest request) {
        return ResponseEntity.ok(qrCodeService.generateQRCode(userDetails.getUser(), request));
    }

    @PostMapping("/scan")
    @Operation(summary = "Scan QR code", description = "Decode QR code and return payment data")
    public ResponseEntity<QRCodePaymentData> scanQRCode(@RequestBody String qrData) {
        return ResponseEntity.ok(qrCodeService.decodeQRCode(qrData));
    }

    @PostMapping("/pay")
    @Operation(summary = "Pay via QR code", description = "Make payment using scanned QR code data")
    public ResponseEntity<TransactionResponse> payViaQRCode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody String qrData) {

        QRCodePaymentData paymentData = qrCodeService.decodeQRCode(qrData);

        TransferRequest request = TransferRequest.builder()
                .recipientIdentifier(paymentData.getEmail())
                .amount(paymentData.getAmount())
                .description(paymentData.getDescription())
                .build();

        return ResponseEntity.ok(transactionService.sendMoney(userDetails.getUser(), request));
    }
}
