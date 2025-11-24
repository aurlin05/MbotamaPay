package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.dto.payment.PaymentRequestCreateRequest;
import com.mbotamapay.backend.dto.payment.PaymentRequestResponse;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.service.PaymentRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.mbotamapay.backend.routes.Routes;

@RestController
@RequestMapping(Routes.PAYMENT_REQUESTS)
@RequiredArgsConstructor
@Tag(name = "Payment Requests", description = "Money request operations")
public class PaymentRequestController {

    private final PaymentRequestService paymentRequestService;

    @PostMapping
    @Operation(summary = "Create payment request", description = "Request money from another user")
    public ResponseEntity<PaymentRequestResponse> createRequest(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PaymentRequestCreateRequest request) {
        return ResponseEntity.ok(paymentRequestService.createPaymentRequest(user, request));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept payment request", description = "Accept and pay a pending request")
    public ResponseEntity<PaymentRequestResponse> acceptRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentRequestService.acceptPaymentRequest(id, user));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject payment request", description = "Reject a pending request")
    public ResponseEntity<PaymentRequestResponse> rejectRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentRequestService.rejectPaymentRequest(id, user));
    }

    @GetMapping("/received")
    @Operation(summary = "Get received requests", description = "Get payment requests sent to me")
    public ResponseEntity<Page<PaymentRequestResponse>> getReceivedRequests(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(paymentRequestService.getReceivedRequests(user, pageable));
    }

    @GetMapping("/sent")
    @Operation(summary = "Get sent requests", description = "Get payment requests I sent")
    public ResponseEntity<Page<PaymentRequestResponse>> getSentRequests(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(paymentRequestService.getSentRequests(user, pageable));
    }
}
