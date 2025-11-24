package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.dto.admin.AdminStatsResponse;
import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.dto.user.UserResponse;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.security.CustomUserDetails;
import com.mbotamapay.backend.service.AdminService;
import com.mbotamapay.backend.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mbotamapay.backend.routes.Routes;

@RestController
@RequestMapping(Routes.ADMIN)
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin operations")
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Get all users (admin only)")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        User admin = getCurrentAdmin();
        List<UserResponse> users = adminService.getAllUsers();
        
        Map<String, Object> details = new HashMap<>();
        details.put("action", "GET_ALL_USERS");
        details.put("userCount", users.size());
        
        auditService.logAdminAction("GET_ALL_USERS", admin, details);
        
        return ResponseEntity.ok(users);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all transactions", description = "Get all transactions (admin only)")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        User admin = getCurrentAdmin();
        List<TransactionResponse> transactions = adminService.getAllTransactions();
        
        Map<String, Object> details = new HashMap<>();
        details.put("action", "GET_ALL_TRANSACTIONS");
        details.put("transactionCount", transactions.size());
        
        auditService.logAdminAction("GET_ALL_TRANSACTIONS", admin, details);
        
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get platform statistics", description = "Get platform stats (admin only)")
    public ResponseEntity<AdminStatsResponse> getStats() {
        User admin = getCurrentAdmin();
        AdminStatsResponse stats = adminService.getStats();
        
        Map<String, Object> details = new HashMap<>();
        details.put("action", "GET_PLATFORM_STATS");
        
        auditService.logAdminAction("GET_PLATFORM_STATS", admin, details);
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/users/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ban user", description = "Deactivate a user account")
    public ResponseEntity<String> banUser(@PathVariable Long id) {
        User admin = getCurrentAdmin();
        adminService.banUser(id);
        
        Map<String, Object> details = new HashMap<>();
        details.put("action", "BAN_USER");
        details.put("targetUserId", id);
        
        auditService.logAdminAction("BAN_USER", admin, details);
        
        return ResponseEntity.ok("User banned successfully");
    }

    @PostMapping("/users/{id}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unban user", description = "Reactivate a user account")
    public ResponseEntity<String> unbanUser(@PathVariable Long id) {
        User admin = getCurrentAdmin();
        adminService.unbanUser(id);
        
        Map<String, Object> details = new HashMap<>();
        details.put("action", "UNBAN_USER");
        details.put("targetUserId", id);
        
        auditService.logAdminAction("UNBAN_USER", admin, details);
        
        return ResponseEntity.ok("User unbanned successfully");
    }
    
    /**
     * Get the current authenticated admin user from SecurityContext
     */
    private User getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getUser();
        }
        return null;
    }
}
