package com.quoteguard.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quoteguard.service.DashboardService;

import lombok.RequiredArgsConstructor;

/**
 * Thin controller delegating to DashboardService. Previously this class
 * queried ClientRepository/InvoiceRepository directly and duplicated
 * DashboardService's getStats() logic line-for-line - DashboardService was
 * never actually injected anywhere, making it dead code with a live
 * duplicate sitting next to it. Consolidated to a single implementation.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats(@AuthenticationPrincipal(expression = "id") Long userId) {
        return dashboardService.getStats(userId);
    }
}
