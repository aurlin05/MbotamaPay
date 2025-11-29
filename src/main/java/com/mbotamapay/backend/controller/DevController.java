package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.entity.Role;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
@Slf4j
public class DevController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

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

    /**
     * Unlock a specific user account by email
     * Resets failed login attempts and clears lock timestamp
     */
    @PostMapping("/unlock-account/{email}")
    public ResponseEntity<Map<String, Object>> unlockAccount(@PathVariable String email) {
        log.info("Attempting to unlock account for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Reset lock fields
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setActive(true);

        userRepository.save(user);

        log.info("Successfully unlocked account for: {} (role: {})", email, user.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Account unlocked successfully");
        response.put("email", email);
        response.put("role", user.getRole());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Unlock all ADMIN accounts
     * Useful for recovering from mass lockouts during development/testing
     */
    @PostMapping("/unlock-all-admins")
    public ResponseEntity<Map<String, Object>> unlockAllAdmins() {
        log.info("Attempting to unlock all ADMIN accounts");

        List<User> adminUsers = userRepository.findByRole(Role.ADMIN);
        int unlockedCount = 0;

        for (User admin : adminUsers) {
            if (admin.getFailedLoginAttempts() > 0 || admin.getLockedUntil() != null || !admin.isActive()) {
                admin.setFailedLoginAttempts(0);
                admin.setLockedUntil(null);
                admin.setActive(true);
                userRepository.save(admin);
                unlockedCount++;
                log.info("Unlocked admin account: {}", admin.getEmail());
            }
        }

        log.info("Successfully unlocked {} out of {} admin accounts", unlockedCount, adminUsers.size());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin accounts unlocked successfully");
        response.put("totalAdmins", adminUsers.size());
        response.put("unlockedCount", unlockedCount);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Get status of all admin accounts
     * Shows which accounts are locked or have failed login attempts
     */
    @GetMapping("/admin-accounts-status")
    public ResponseEntity<Map<String, Object>> getAdminAccountsStatus() {
        log.info("Fetching status of all ADMIN accounts");

        List<User> adminUsers = userRepository.findByRole(Role.ADMIN);

        List<Map<String, Object>> adminStatuses = adminUsers.stream()
                .map(admin -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("email", admin.getEmail());
                    status.put("name", admin.getFirstName() + " " + admin.getLastName());
                    status.put("active", admin.isActive());
                    status.put("failedLoginAttempts", admin.getFailedLoginAttempts());
                    status.put("lockedUntil", admin.getLockedUntil());
                    status.put("isCurrentlyLocked", admin.getLockedUntil() != null &&
                            admin.getLockedUntil().isAfter(LocalDateTime.now()));
                    return status;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalAdmins", adminUsers.size());
        response.put("admins", adminStatuses);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Clear all Redis cache
     * Useful after configuration changes to avoid serialization conflicts
     */
    @PostMapping("/clear-all-cache")
    public ResponseEntity<Map<String, Object>> clearAllCache() {
        log.warn("Request to clear ALL Redis cache - this will remove all cached data");

        try {
            // This requires RedisTemplate or CacheManager injection
            // For now, return a message indicating manual action needed
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Please restart Redis or run 'redis-cli FLUSHALL' to clear the cache");
            response.put("reason", "Cache format changed - old cached data is incompatible");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing cache", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(500).body(response);
        }
    }
}
