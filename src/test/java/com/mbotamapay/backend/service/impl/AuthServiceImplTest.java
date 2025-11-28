package com.mbotamapay.backend.service.impl;

import com.mbotamapay.backend.dto.auth.AuthResponse;
import com.mbotamapay.backend.dto.auth.RegisterRequest;
import com.mbotamapay.backend.entity.KycLevel;
import com.mbotamapay.backend.entity.Role;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.exception.BusinessException;
import com.mbotamapay.backend.repository.UserRepository;
import com.mbotamapay.backend.security.JwtService;
import com.mbotamapay.backend.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private WalletService walletService;

    @Mock
    private com.mbotamapay.backend.service.ReferralService referralService;

    @Mock
    private com.mbotamapay.backend.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private com.mbotamapay.backend.service.EmailService emailService;

    @Mock
    private com.mbotamapay.backend.service.AuditService auditService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .phone("1234567890")
                .password("password123")
                .build();
    }

    @Test
    void register_ShouldCreateUserAndWallet_WhenValidRequest() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(registerRequest.getPhone())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword");
        when(referralService.generateReferralCode()).thenReturn("REF123");

        User savedUser = User.builder()
                .id(1L)
                .name(registerRequest.getFirstName() + " " + registerRequest.getLastName())
                .email(registerRequest.getEmail())
                .phone(registerRequest.getPhone())
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .kycLevel(KycLevel.LEVEL_1)
                .active(true)
                .referralCode("REF123")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(registerRequest.getFirstName(), response.getUser().getFirstName());
        assertEquals(registerRequest.getLastName(), response.getUser().getLastName());
        assertEquals(registerRequest.getEmail(), response.getUser().getEmail());
        verify(walletService, times(1)).createWallet(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenPhoneExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(registerRequest.getPhone())).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }
}
