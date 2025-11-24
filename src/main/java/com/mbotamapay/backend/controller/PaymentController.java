package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.dto.payment.CinetPayCallback;
import com.mbotamapay.backend.dto.payment.PaymentInitRequest;
import com.mbotamapay.backend.dto.payment.PaymentInitResponse;
import com.mbotamapay.backend.security.CustomUserDetails;
import com.mbotamapay.backend.service.CinetPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.mbotamapay.backend.routes.Routes;

@RestController
@RequestMapping(Routes.PAYMENT)
@RequiredArgsConstructor
@Tag(name = "Payment", description = "CinetPay payment APIs")
public class PaymentController {

    private final CinetPayService cinetPayService;

    @PostMapping("/init")
    @Operation(summary = "Initialize payment", description = "Initialize a top-up payment via CinetPay")
    public ResponseEntity<PaymentInitResponse> initializePayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentInitRequest request) {
        return ResponseEntity.ok(cinetPayService.initializePayment(userDetails.getUser(), request));
    }

    @PostMapping("/callback")
    @Operation(summary = "Payment callback", description = "CinetPay callback endpoint")
    public ResponseEntity<String> handleCallback(@RequestBody CinetPayCallback callback) {
        cinetPayService.handleCallback(callback);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/return")
    @Operation(summary = "Payment return", description = "CinetPay return URL")
    public ResponseEntity<String> handleReturn(@RequestParam String transaction_id) {
        return ResponseEntity.ok("Payment processed for transaction: " + transaction_id);
    }
}
