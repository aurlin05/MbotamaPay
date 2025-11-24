package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.entity.KycDocument;
import com.mbotamapay.backend.routes.Routes;
import com.mbotamapay.backend.security.CustomUserDetails;
import com.mbotamapay.backend.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(Routes.KYC)
@RequiredArgsConstructor
@Tag(name = "KYC", description = "Know Your Customer operations")
public class KycController {

    private final KycService kycService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload KYC document", description = "Upload ID or Passport")
    public ResponseEntity<KycDocument> uploadDocument(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) throws IOException {
        return ResponseEntity.ok(kycService.uploadDocument(userDetails.getUser(), file, type));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending documents", description = "Admin only")
    public ResponseEntity<List<KycDocument>> getPendingDocuments() {
        return ResponseEntity.ok(kycService.getPendingDocuments());
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Verify document", description = "Approve or reject document")
    public ResponseEntity<String> verifyDocument(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestParam boolean approved) {
        kycService.verifyDocument(id, approved, userDetails.getUsername());
        return ResponseEntity.ok(approved ? "Document approved" : "Document rejected");
    }
}
