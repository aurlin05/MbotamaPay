package com.mbotamapay.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
public class DevController {

    private final PasswordEncoder passwordEncoder;

    @GetMapping("/hash-password")
    public String hashPassword(@RequestParam String password) {
        return passwordEncoder.encode(password);
    }

    @GetMapping("/verify-password")
    public Map<String, Object> verifyPassword(
            @RequestParam String rawPassword,
            @RequestParam String hash) {
        Map<String, Object> result = new HashMap<>();
        boolean matches = passwordEncoder.matches(rawPassword, hash);
        result.put("matches", matches);
        result.put("rawPassword", rawPassword);
        result.put("hash", hash);
        return result;
    }
}
