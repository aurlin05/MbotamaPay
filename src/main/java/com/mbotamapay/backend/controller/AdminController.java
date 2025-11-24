package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.dto.admin.AdminStatsResponse;
import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.dto.user.UserResponse;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.security.CustomUserDetails;
import com.mbotamapay.backend.service.AdminService;
import com.mbotamapay.backend.service.AuditService;
import com.mbotamapay.backend.service.LiquidityManager;
import com.mbotamapay.backend.repository.OperatorAccountRepository;
import com.mbotamapay.backend.entity.OperatorAccount;
import com.mbotamapay.backend.dto.liquidity.RebalanceSuggestion;
import com.mbotamapay.backend.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    private final LiquidityManager liquidityManager;
    private final OperatorAccountRepository operatorAccountRepository;

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

    @GetMapping("/operator/balances")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get operator account balances", description = "Get all operator account balances")
    public ResponseEntity<List<OperatorAccount>> getOperatorBalances() {
        User admin = getCurrentAdmin();
        List<OperatorAccount> accounts = operatorAccountRepository.findAll();

        Map<String, Object> details = new HashMap<>();
        details.put("action", "GET_OPERATOR_BALANCES");
        auditService.logAdminAction("GET_OPERATOR_BALANCES", admin, details);

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/operator/rebalance-suggestions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get rebalancing suggestions", description = "Get liquidity rebalancing suggestions")
    public ResponseEntity<RebalanceSuggestion> getRebalanceSuggestions() {
        User admin = getCurrentAdmin();
        RebalanceSuggestion suggestions = liquidityManager.suggestRebalancing();

        Map<String, Object> details = new HashMap<>();
        details.put("action", "GET_REBALANCE_SUGGESTIONS");
        details.put("suggestionsCount", suggestions.getActions().size());
        auditService.logAdminAction("GET_REBALANCE_SUGGESTIONS", admin, details);

        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/operator/topup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Topup operator account", description = "Simulate topup of operator account")
    public ResponseEntity<String> topupOperatorAccount(
            @RequestParam OperatorAccount.Provider provider,
            @RequestParam String currency,
            @RequestParam BigDecimal amount) {
        User admin = getCurrentAdmin();

        OperatorAccount account = operatorAccountRepository
                .findByProviderAndCurrency(provider, currency)
                .orElseThrow(() -> new BusinessException("Operator account not found"));

        account.setBalance(account.getBalance().add(amount));
        operatorAccountRepository.save(account);

        Map<String, Object> details = new HashMap<>();
        details.put("action", "TOPUP_OPERATOR_ACCOUNT");
        details.put("provider", provider);
        details.put("currency", currency);
        details.put("amount", amount);
        auditService.logAdminAction("TOPUP_OPERATOR_ACCOUNT", admin, details);

        return ResponseEntity.ok("Operator account topped up successfully");
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
