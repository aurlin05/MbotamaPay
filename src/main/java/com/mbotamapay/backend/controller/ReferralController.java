package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
public class ReferralController {

    @GetMapping("/code")
    public ResponseEntity<Map<String, String>> getMyReferralCode(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(Map.of("referralCode", userDetails.getUser().getReferralCode()));
    }

    // Future: Add endpoint to get referral stats (count, earnings)
}
