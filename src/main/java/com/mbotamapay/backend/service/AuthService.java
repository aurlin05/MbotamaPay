package com.mbotamapay.backend.service;

import com.mbotamapay.backend.dto.auth.AuthResponse;
import com.mbotamapay.backend.dto.auth.LoginRequest;
import com.mbotamapay.backend.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}
