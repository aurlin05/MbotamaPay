package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.dto.wallet.WalletResponse;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.security.CustomUserDetails;
import com.mbotamapay.backend.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mbotamapay.backend.routes.Routes;

@RestController
@RequestMapping(Routes.WALLET)
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Wallet management APIs")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/balance")
    @Operation(summary = "Get wallet balance", description = "Returns the current user's wallet balance")
    public ResponseEntity<WalletResponse> getBalance(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        return ResponseEntity.ok(walletService.getBalance(user.getId()));
    }
}
