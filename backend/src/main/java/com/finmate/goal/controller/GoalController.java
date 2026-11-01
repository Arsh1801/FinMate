package com.finmate.goal.controller;

import com.finmate.common.util.MoneyUtils;
import com.finmate.common.util.SecurityUtils;
import com.finmate.goal.dto.*;
import com.finmate.goal.entity.GoalStatus;
import com.finmate.goal.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/goals")
@Tag(name = "Savings Goals", description = "Endpoints for personal savings goals and contribution tracking")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    @Operation(summary = "List savings goals with calculated progress")
    public ResponseEntity<List<SavingsGoalDto>> getGoals(@RequestParam(required = false) GoalStatus status) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(goalService.getGoals(userId, status));
    }

    @PostMapping
    @Operation(summary = "Create a new savings goal")
    public ResponseEntity<SavingsGoalDto> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SavingsGoalDto created = goalService.createGoal(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/goals/" + created.getId())).body(created);
    }

    @GetMapping("/free-balance")
    @Operation(summary = "Get available free liquidity for goal contributions")
    public ResponseEntity<Map<String, Object>> getFreeBalance() {
        Long userId = SecurityUtils.getCurrentUserId();
        BigDecimal freeBalance = goalService.calculateFreeBalance(userId);
        return ResponseEntity.ok(Map.of(
                "freeBalance", freeBalance.toPlainString(),
                "formattedFreeBalance", MoneyUtils.formatIndian(freeBalance)
        ));
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "Get single savings goal with progress")
    public ResponseEntity<SavingsGoalDto> getGoal(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(goalService.getGoal(userId, id));
    }

    @PutMapping("/{id:[0-9]+}")
    @Operation(summary = "Edit savings goal (ACTIVE goals only)")
    public ResponseEntity<SavingsGoalDto> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGoalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(goalService.updateGoal(userId, id, request));
    }

    @PatchMapping("/{id:[0-9]+}/status")
    @Operation(summary = "Update goal status (COMPLETED or ARCHIVED)")
    public ResponseEntity<SavingsGoalDto> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody GoalStatusRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(goalService.setGoalStatus(userId, id, request.getStatus()));
    }

    @GetMapping("/{id:[0-9]+}/contributions")
    @Operation(summary = "Get contribution history for a goal (paginated, newest first)")
    public ResponseEntity<Page<ContributionDto>> getContributions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(goalService.getContributions(userId, id, page, size));
    }

    @PostMapping("/{id:[0-9]+}/contributions")
    @Operation(summary = "Add a contribution to an active goal")
    public ResponseEntity<ContributionResponseDto> addContribution(
            @PathVariable Long id,
            @Valid @RequestBody AddContributionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ContributionResponseDto response = goalService.addContribution(userId, id, request);
        return ResponseEntity.created(URI.create("/api/v1/goals/" + id + "/contributions/" + response.getContribution().getId()))
                .body(response);
    }

    @DeleteMapping("/{id:[0-9]+}/contributions/{contributionId:[0-9]+}")
    @Operation(summary = "Delete a contribution from an active goal")
    public ResponseEntity<Void> deleteContribution(
            @PathVariable Long id,
            @PathVariable Long contributionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        goalService.deleteContribution(userId, id, contributionId);
        return ResponseEntity.noContent().build();
    }
}
