package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.routes.Routes;
import com.mbotamapay.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Routes.DASHBOARD)
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Admin statistics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get dashboard stats", description = "Get aggregated platform statistics")
    public ResponseEntity<DashboardService.DashboardStats> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}
