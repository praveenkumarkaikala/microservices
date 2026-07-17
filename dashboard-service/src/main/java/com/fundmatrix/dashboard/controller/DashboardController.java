package com.fundmatrix.dashboard.controller;

import com.fundmatrix.dashboard.dto.AdminStatsDto;
import com.fundmatrix.dashboard.dto.ComplianceSummaryDto;
import com.fundmatrix.dashboard.dto.DistributorDashboardDto;
import com.fundmatrix.dashboard.dto.InvestorDashboardDto;
import com.fundmatrix.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboards", description = "Role-specific aggregated read models")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/investor")
    public InvestorDashboardDto investor() {
        return dashboardService.investorDashboard();
    }

    @GetMapping("/distributor")
    public DistributorDashboardDto distributor() {
        return dashboardService.distributorDashboard();
    }

    @GetMapping("/admin")
    public AdminStatsDto admin() {
        return dashboardService.adminStats();
    }

    @GetMapping("/compliance")
    public ComplianceSummaryDto compliance() {
        return dashboardService.complianceSummary();
    }
}
