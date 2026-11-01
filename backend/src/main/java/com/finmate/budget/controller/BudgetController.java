package com.finmate.budget.controller;

import com.finmate.budget.dto.*;
import com.finmate.budget.service.BudgetService;
import com.finmate.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/budgets")
@Tag(name = "Budgets", description = "Endpoints for monthly overall and category budget management")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    @Operation(summary = "List budgets for a month with calculated spent, remaining, utilization, and status")
    public ResponseEntity<List<BudgetDto>> getBudgets(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.getBudgetsForMonth(userId, year, month));
    }

    @PostMapping
    @Operation(summary = "Create an overall or category monthly budget")
    public ResponseEntity<BudgetDto> createBudget(@Valid @RequestBody CreateBudgetRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        BudgetDto created = budgetService.createBudget(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/budgets/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update budget limit or alert threshold (current or future month only)")
    public ResponseEntity<BudgetDto> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBudgetRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.updateBudget(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a budget (current or future month only)")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        budgetService.deleteBudget(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    @Operation(summary = "Get monthly budget performance history")
    public ResponseEntity<List<BudgetHistoryDto>> getBudgetHistory(
            @RequestParam(defaultValue = "12") Integer months,
            @RequestParam(required = false) Long categoryId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.getBudgetHistory(userId, months, categoryId));
    }

    @PostMapping("/copy")
    @Operation(summary = "Copy budgets from one month to another")
    public ResponseEntity<Map<String, Integer>> copyBudgets(@Valid @RequestBody CopyBudgetsRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Integer> result = budgetService.copyBudgets(userId, request);
        return ResponseEntity.status(201).body(result);
    }
}
