package com.finmate.transaction.controller;

import com.finmate.common.util.SecurityUtils;
import com.finmate.transaction.dto.CreateRecurringRuleRequest;
import com.finmate.transaction.dto.RecurringRuleDto;
import com.finmate.transaction.service.RecurringRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/recurring-rules")
@Tag(name = "Recurring Rules", description = "Endpoints for managing automated recurring transactions")
public class RecurringRuleController {

    private final RecurringRuleService recurringRuleService;

    public RecurringRuleController(RecurringRuleService recurringRuleService) {
        this.recurringRuleService = recurringRuleService;
    }

    @GetMapping
    @Operation(summary = "List all recurring rules for authenticated user")
    public ResponseEntity<List<RecurringRuleDto>> getRules() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(recurringRuleService.getRulesForUser(userId));
    }

    @PostMapping
    @Operation(summary = "Create a new recurring rule")
    public ResponseEntity<RecurringRuleDto> createRule(@Valid @RequestBody CreateRecurringRuleRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        RecurringRuleDto created = recurringRuleService.createRule(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/recurring-rules/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing recurring rule")
    public ResponseEntity<RecurringRuleDto> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody CreateRecurringRuleRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(recurringRuleService.updateRule(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Stop and delete a recurring rule (past transactions remain)")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        recurringRuleService.deleteRule(userId, id);
        return ResponseEntity.noContent().build();
    }
}
