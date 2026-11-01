package com.finmate.dashboard.controller;

import com.finmate.common.security.UserPrincipal;
import com.finmate.dashboard.dto.*;
import com.finmate.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Dashboard analytics, KPIs, charts, and smart insights")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get high-level financial KPIs with period deltas")
    public ResponseEntity<DashboardSummaryDto> getSummary(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "period", defaultValue = "this_month") String period) {
        return ResponseEntity.ok(dashboardService.getSummary(currentUser.getId(), period));
    }

    @GetMapping("/monthly-bar")
    @Operation(summary = "Get income vs expense historical comparison for N months")
    public ResponseEntity<List<MonthlyIncomeExpenseDto>> getMonthlyBar(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "months", defaultValue = "6") int months) {
        return ResponseEntity.ok(dashboardService.getMonthlyIncomeExpense(currentUser.getId(), months));
    }

    @GetMapping("/category-donut")
    @Operation(summary = "Get category spending breakdown with percentages for donut chart")
    public ResponseEntity<List<CategoryDistributionDto>> getCategoryDonut(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "month", required = false) String month) {
        return ResponseEntity.ok(dashboardService.getCategoryDistribution(currentUser.getId(), month));
    }

    @GetMapping("/spending-trend")
    @Operation(summary = "Get daily spending trajectory over the last N days")
    public ResponseEntity<List<SpendingTrendDto>> getSpendingTrend(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "days", defaultValue = "30") int days) {
        return ResponseEntity.ok(dashboardService.getSpendingTrends(currentUser.getId(), days));
    }

    @GetMapping("/budget-bars")
    @Operation(summary = "Get budget utilization progress bars for the specified month")
    public ResponseEntity<List<BudgetUtilizationBarDto>> getBudgetBars(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month) {
        return ResponseEntity.ok(dashboardService.getBudgetUtilizationBars(currentUser.getId(), year, month));
    }

    @GetMapping("/insights")
    @Operation(summary = "Get rule-based contextual spending and budget insights")
    public ResponseEntity<List<RuleBasedInsightDto>> getInsights(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(dashboardService.getRuleBasedInsights(currentUser.getId()));
    }

    @GetMapping("/group-summary")
    @Operation(summary = "Get shared groups net balance widget overview")
    public ResponseEntity<GroupSummaryDto> getGroupSummary(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(dashboardService.getGroupSummary(currentUser.getId()));
    }
}
