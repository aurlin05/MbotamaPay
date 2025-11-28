package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.auth.AuthResponse;
import com.mbotamapay.backend.dto.auth.LoginRequest;
import com.mbotamapay.backend.dto.auth.RegisterRequest;
import com.mbotamapay.backend.entity.KycLevel;
import com.mbotamapay.backend.entity.Role;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.repository.UserRepository;
import com.mbotamapay.backend.security.CustomUserDetails;
import com.mbotamapay.backend.security.JwtService;
import com.mbotamapay.backend.service.AuthService;
import com.mbotamapay.backend.service.ReferralService;
import com.mbotamapay.backend.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ReferralService referralService;
    private final com.mbotamapay.backend.repository.PasswordResetTokenRepository passwordResetTokenRepository;
    private final com.mbotamapay.backend.repository.RefreshTokenRepository refreshTokenRepository;
    private final com.mbotamapay.backend.service.EmailService emailService;
    private final WalletService walletService;
    private final com.mbotamapay.backend.service.AuditService auditService;

    @org.springframework.beans.factory.annotation.Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpiration;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Phone number already exists");
        }

        User referrer = null;
        if (request.getReferralCode() != null && !request.getReferralCode().isEmpty()) {
            referrer = referralService.findByReferralCode(request.getReferralCode())
                    .orElse(null);
        }

        User user = User.builder()
                .name(request.getFirstName() + " " + request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .kycLevel(KycLevel.LEVEL_1)
                .active(true)
                .referralCode(referralService.generateReferralCode())
                .referrer(referrer)
                .build();

        user = userRepository.save(user);

        // Auto-create wallet
        walletService.createWallet(user);

        String token = jwtService.generateToken(new CustomUserDetails(user));
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getIdentifier());

        User user = userRepository.findByEmail(request.getIdentifier())
                .or(() -> userRepository.findByPhone(request.getIdentifier()))
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
            // Log account locked security event
            java.util.Map<String, Object> details = new java.util.HashMap<>();
            details.put("userId", user.getId());
            details.put("email", user.getEmail());
            details.put("lockedUntil", user.getLockedUntil().toString());
            details.put("reason", "Account locked due to multiple failed login attempts");
            auditService.logSecurityEvent("ACCOUNT_LOCKED_ACCESS_ATTEMPT", "WARNING", details);

            throw new BusinessException("Account is locked. Try again later.");
        }

        // DEBUG: Log password details
        log.info("DEBUG - User found: {}, Password hash from DB: {}", user.getEmail(), user.getPasswordHash());
        log.info("DEBUG - Raw password from request: {}", request.getPassword());
        log.info("DEBUG - Testing password match: {}",
                passwordEncoder.matches(request.getPassword(), user.getPasswordHash()));

        try {
            log.info("DEBUG - Attempting Spring Security authentication for: {}", user.getEmail());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword()));
            log.info("DEBUG - Authentication SUCCESS!");

            // Reset failed attempts on success
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }

            // Log successful login
            java.util.Map<String, Object> successDetails = new java.util.HashMap<>();
            successDetails.put("userId", user.getId());
            successDetails.put("email", user.getEmail());
            successDetails.put("loginMethod", "PASSWORD");
            auditService.logSecurityEvent("LOGIN_SUCCESS", "INFO", successDetails);

        } catch (org.springframework.security.core.AuthenticationException e) {
            log.error("DEBUG - Authentication FAILED! Exception: {}", e.getMessage(), e);

            // Increment failed attempts
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            // Log failed login attempt
            java.util.Map<String, Object> failDetails = new java.util.HashMap<>();
            failDetails.put("userId", user.getId());
            failDetails.put("email", user.getEmail());
            failDetails.put("attemptNumber", attempts);
            failDetails.put("identifier", request.getIdentifier());

            if (attempts >= 5) {
                user.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(15));
                userRepository.save(user);

                // Log account locked event with critical severity
                failDetails.put("lockedUntil", user.getLockedUntil().toString());
                auditService.logSecurityEvent("ACCOUNT_LOCKED", "CRITICAL", failDetails);

                throw new BusinessException("Account locked due to too many failed attempts.");
            }

            userRepository.save(user);

            // Log failed login with appropriate severity
            String severity = attempts >= 3 ? "WARNING" : "INFO";
            auditService.logSecurityEvent("LOGIN_FAILED", severity, failDetails);

            throw new BusinessException("Invalid credentials");
        }

        String token = jwtService.generateToken(new CustomUserDetails(user));
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        // Verify token exists and is valid
        // Since we store hash, we need to find by hash?
        // No, we can't find by hash if we don't know the token.
        // Wait, if we hash the token, we can't lookup by raw token unless we hash it
        // first.
        // Yes, we hash the incoming token and look it up.

        // But wait, if I generate a random UUID, I give it to the user.
        // When user sends it back, I hash it and look up.

        // However, standard practice with opaque tokens is just storing them.
        // If I hash them, I need to be able to reproduce the hash.
        // BCrypt is salted, so I can't reproduce the hash easily for lookup without
        // iterating (bad).
        // So for lookup, I should use SHA256 (fast, deterministic) or just store the
        // token if it's a UUID (low risk if DB leaked compared to passwords).
        // The entity uses `tokenHash`. If I used BCrypt there, I can't lookup.
        // I should probably use a fast hash or just store it.
        // Given the entity name `tokenHash`, I'll assume I should use SHA256 or
        // similar.
        // OR, I can use the ID of the token + the secret part.

        // Let's assume for now I'll use SHA256 for the hash.

        String tokenHash = hashToken(refreshToken);

        com.mbotamapay.backend.entity.RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (storedToken.isExpired() || storedToken.getRevoked()) {
            throw new BusinessException("Refresh token expired or revoked");
        }

        User user = storedToken.getUser();
        String newToken = jwtService.generateToken(new CustomUserDetails(user));

        // Rotate refresh token
        revokeRefreshToken(storedToken);
        String newRefreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(this::revokeRefreshToken);
    }

    private String createRefreshToken(User user) {
        String token = java.util.UUID.randomUUID().toString();
        String tokenHash = hashToken(token);

        com.mbotamapay.backend.entity.RefreshToken refreshToken = com.mbotamapay.backend.entity.RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(java.time.LocalDateTime.now().plusNanos(refreshExpiration * 1000000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private void revokeRefreshToken(com.mbotamapay.backend.entity.RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    private String hashToken(String token) {
        // Use SHA-256 for deterministic hashing
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found with email: " + email));

        // Delete existing token if any
        passwordResetTokenRepository.deleteByUser(user);

        String token = java.util.UUID.randomUUID().toString();
        com.mbotamapay.backend.entity.PasswordResetToken resetToken = com.mbotamapay.backend.entity.PasswordResetToken
                .builder()
                .token(token)
                .user(user)
                .expiryDate(java.time.LocalDateTime.now().plusMinutes(15)) // 15 mins expiry
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send email
        emailService.sendEmail(user.getEmail(), "Password Reset Request",
                "To reset your password, use this token: " + token);

        // Log password reset request
        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("userId", user.getId());
        details.put("email", user.getEmail());
        details.put("tokenExpiry", resetToken.getExpiryDate().toString());
        auditService.logSecurityEvent("PASSWORD_RESET_REQUESTED", "WARNING", details);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        com.mbotamapay.backend.entity.PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Invalid password reset token"));

        if (resetToken.isExpired()) {
            // Log expired token usage attempt
            java.util.Map<String, Object> expiredDetails = new java.util.HashMap<>();
            expiredDetails.put("userId", resetToken.getUser().getId());
            expiredDetails.put("email", resetToken.getUser().getEmail());
            expiredDetails.put("tokenExpiry", resetToken.getExpiryDate().toString());
            auditService.logSecurityEvent("PASSWORD_RESET_EXPIRED_TOKEN", "WARNING", expiredDetails);

            throw new BusinessException("Token has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);

        // Log successful password reset
        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("userId", user.getId());
        details.put("email", user.getEmail());
        auditService.logSecurityEvent("PASSWORD_RESET_COMPLETED", "WARNING", details);
    }

    private com.mbotamapay.backend.dto.user.UserResponse mapToUserResponse(User user) {
        String[] nameParts = user.getName().split(" ", 2);
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        return com.mbotamapay.backend.dto.user.UserResponse.builder()
                .id(user.getId())
                .firstName(firstName)
                .lastName(lastName)
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .kycLevel(user.getKycLevel().name())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
