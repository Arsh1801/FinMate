package com.finmate.settlement.controller;

import com.finmate.common.util.SecurityUtils;
import com.finmate.settlement.dto.RecordSettlementRequest;
import com.finmate.settlement.dto.SettlementActionRequest;
import com.finmate.settlement.dto.SettlementDto;
import com.finmate.settlement.entity.SettlementStatus;
import com.finmate.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Settlements", description = "Endpoints for recording and confirming group debt settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/groups/{groupId}/settlements")
    @Operation(summary = "Record a settlement payment between group members")
    public ResponseEntity<SettlementDto> recordSettlement(
            @PathVariable Long groupId,
            @Valid @RequestBody RecordSettlementRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SettlementDto created = settlementService.recordSettlement(userId, groupId, request);
        return ResponseEntity.created(URI.create("/api/v1/groups/" + groupId + "/settlements/" + created.getId())).body(created);
    }

    @GetMapping("/groups/{groupId}/settlements")
    @Operation(summary = "List settlements in a group")
    public ResponseEntity<Page<SettlementDto>> getSettlements(
            @PathVariable Long groupId,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(settlementService.getGroupSettlements(userId, groupId, status, page, size));
    }

    @PatchMapping("/groups/{groupId}/settlements/{settlementId}")
    @Operation(summary = "Confirm, reject, or reverse a settlement (Receiver only)")
    public ResponseEntity<SettlementDto> processSettlementAction(
            @PathVariable Long groupId,
            @PathVariable Long settlementId,
            @Valid @RequestBody SettlementActionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(settlementService.processSettlementAction(userId, groupId, settlementId, request));
    }

    @PostMapping("/groups/{groupId}/settlements/{settlementId}/confirm")
    @Operation(summary = "Confirm receipt of settlement transfer")
    public ResponseEntity<SettlementDto> confirmSettlement(
            @PathVariable Long groupId,
            @PathVariable Long settlementId) {
        Long userId = SecurityUtils.getCurrentUserId();
        SettlementActionRequest request = new SettlementActionRequest();
        request.setAction("CONFIRM");
        return ResponseEntity.ok(settlementService.processSettlementAction(userId, groupId, settlementId, request));
    }

    @PostMapping("/groups/{groupId}/settlements/{settlementId}/reject")
    @Operation(summary = "Reject a settlement transfer")
    public ResponseEntity<SettlementDto> rejectSettlement(
            @PathVariable Long groupId,
            @PathVariable Long settlementId) {
        Long userId = SecurityUtils.getCurrentUserId();
        SettlementActionRequest request = new SettlementActionRequest();
        request.setAction("REJECT");
        return ResponseEntity.ok(settlementService.processSettlementAction(userId, groupId, settlementId, request));
    }

    @GetMapping({"/settlements/pending", "/groups/settlements/pending-for-me"})
    @Operation(summary = "Get pending settlements across all groups awaiting my confirmation")
    public ResponseEntity<List<SettlementDto>> getPendingSettlements() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(settlementService.getPendingSettlementsForUser(userId));
    }
}
