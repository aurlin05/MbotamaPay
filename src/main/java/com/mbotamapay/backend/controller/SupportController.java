package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.dto.support.CreateTicketRequest;
import com.mbotamapay.backend.entity.SupportTicket;
import com.mbotamapay.backend.routes.Routes;
import com.mbotamapay.backend.security.CustomUserDetails;
import com.mbotamapay.backend.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Routes.SUPPORT)
@RequiredArgsConstructor
@Tag(name = "Support", description = "Support ticket management")
public class SupportController {

    private final SupportTicketService supportTicketService;

    @PostMapping("/tickets")
    @Operation(summary = "Create ticket", description = "Create a new support ticket")
    public ResponseEntity<SupportTicket> createTicket(@AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.ok(supportTicketService.createTicket(userDetails.getUser(), request));
    }

    @GetMapping("/tickets")
    @Operation(summary = "Get my tickets", description = "Get all tickets for current user")
    public ResponseEntity<List<SupportTicket>> getMyTickets(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(supportTicketService.getUserTickets(userDetails.getUser()));
    }

    // Admin endpoints could be added here or in a separate AdminController
}
