package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.dto.auth.AuthResponse;
import com.mbotamapay.backend.dto.auth.LoginRequest;
import com.mbotamapay.backend.dto.auth.RegisterRequest;
import com.mbotamapay.backend.dto.auth.VerifyOtpRequest;
import com.mbotamapay.backend.entity.OtpType;
import com.mbotamapay.backend.service.AuthService;
import com.mbotamapay.backend.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mbotamapay.backend.routes.Routes;

@RestController
@RequestMapping(Routes.AUTH)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and wallet")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates user and returns JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh Token", description = "Get new access token using refresh token")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody com.mbotamapay.backend.dto.auth.RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke refresh token")
    public ResponseEntity<String> logout(
            @Valid @RequestBody com.mbotamapay.backend.dto.auth.RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verifies the OTP code sent to user")
    public ResponseEntity<String> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        otpService.verifyOtp(request.getRecipient(), request.getCode());
        return ResponseEntity.ok("OTP verified successfully");
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP", description = "Resends OTP code to user's email")
    public ResponseEntity<String> resendOtp(@RequestParam String email) {
        otpService.generateAndSendOtp(email, OtpType.EMAIL);
        return ResponseEntity.ok("OTP sent successfully");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Initiates password reset process")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok("Password reset token sent to email");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password", description = "Resets password using token")
    public ResponseEntity<String> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Password reset successfully");
    }
}
