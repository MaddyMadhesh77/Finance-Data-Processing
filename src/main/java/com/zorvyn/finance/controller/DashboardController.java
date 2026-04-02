package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.DashboardSummary;
import com.zorvyn.finance.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for dashboard analytics.
 * Accessible to ADMIN, ANALYST, and VIEWER (configured in SecurityConfig).
 *
 * Endpoints:
 *   GET /api/dashboard/summary -> full aggregated summary
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
