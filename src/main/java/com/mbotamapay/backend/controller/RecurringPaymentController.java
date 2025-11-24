package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.entity.RecurringPayment;
import com.mbotamapay.backend.routes.Routes;
import com.mbotamapay.backend.security.CustomUserDetails;
import com.mbotamapay.backend.service.RecurringPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(Routes.RECURRING_PAYMENTS)
@RequiredArgsConstructor
@Tag(name = "Recurring Payments", description = "Manage scheduled payments")
public class RecurringPaymentController {

    private final RecurringPaymentService recurringPaymentService;

    @PostMapping
    @Operation(summary = "Create recurring payment", description = "Schedule a new recurring payment")
    public ResponseEntity<RecurringPayment> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateRecurringPaymentRequest request) {
        return ResponseEntity.ok(recurringPaymentService.createRecurringPayment(
                userDetails.getUser(),
                request.getRecipientEmail(),
                request.getAmount(),
                request.getFrequency(),
                request.getDescription()));
    }

    @GetMapping
    @Operation(summary = "Get my recurring payments", description = "List active and inactive recurring payments")
    public ResponseEntity<List<RecurringPayment>> getMyPayments(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(recurringPaymentService.getUserRecurringPayments(userDetails.getUser()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel recurring payment", description = "Stop a recurring payment")
    public ResponseEntity<String> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        recurringPaymentService.cancelRecurringPayment(id, userDetails.getUser());
        return ResponseEntity.ok("Recurring payment cancelled");
    }

    @Data
    public static class CreateRecurringPaymentRequest {
        private String recipientEmail;
        private BigDecimal amount;
        private RecurringPayment.Frequency frequency;
        private String description;
    }
}
