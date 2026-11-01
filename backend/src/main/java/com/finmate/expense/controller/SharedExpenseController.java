package com.finmate.expense.controller;

import com.finmate.common.util.SecurityUtils;
import com.finmate.expense.dto.CreateSharedExpenseRequest;
import com.finmate.expense.dto.SharedExpenseDto;
import com.finmate.expense.dto.UpdateSharedExpenseRequest;
import com.finmate.expense.service.SharedExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/expenses")
@Tag(name = "Shared Expenses", description = "Endpoints for recording and splitting group shared expenses")
public class SharedExpenseController {

    private final SharedExpenseService sharedExpenseService;

    public SharedExpenseController(SharedExpenseService sharedExpenseService) {
        this.sharedExpenseService = sharedExpenseService;
    }

    @GetMapping
    @Operation(summary = "List shared expenses in a group with locked status indicator")
    public ResponseEntity<Page<SharedExpenseDto>> getExpenses(
            @PathVariable Long groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long paidBy,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(sharedExpenseService.getGroupExpenses(userId, groupId, from, to, paidBy, q, page, size));
    }

    @PostMapping
    @Operation(summary = "Create shared expense with equal, exact, or percentage split")
    public ResponseEntity<SharedExpenseDto> createExpense(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateSharedExpenseRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SharedExpenseDto created = sharedExpenseService.createExpense(userId, groupId, request);
        return ResponseEntity.created(URI.create("/api/v1/groups/" + groupId + "/expenses/" + created.getId())).body(created);
    }

    @GetMapping("/{expenseId}")
    @Operation(summary = "Get expense detail with individual splits")
    public ResponseEntity<SharedExpenseDto> getExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(sharedExpenseService.getExpense(userId, groupId, expenseId));
    }

    @PutMapping("/{expenseId}")
    @Operation(summary = "Update expense (locked expenses allow editing non-financial fields only)")
    public ResponseEntity<SharedExpenseDto> updateExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId,
            @Valid @RequestBody UpdateSharedExpenseRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(sharedExpenseService.updateExpense(userId, groupId, expenseId, request));
    }

    @DeleteMapping("/{expenseId}")
    @Operation(summary = "Soft-delete shared expense (unlocked expenses only)")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId) {
        Long userId = SecurityUtils.getCurrentUserId();
        sharedExpenseService.deleteExpense(userId, groupId, expenseId);
        return ResponseEntity.noContent().build();
    }
}
