package com.finmate.group.controller;

import com.finmate.common.util.SecurityUtils;
import com.finmate.group.dto.*;
import com.finmate.group.entity.GroupStatus;
import com.finmate.group.service.GroupService;
import com.finmate.settlement.dto.GroupBalancesDto;
import com.finmate.settlement.dto.SettlementSuggestionDto;
import com.finmate.settlement.service.BalanceEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups", description = "Endpoints for shared expense groups and membership")
public class GroupController {

    private final GroupService groupService;
    private final BalanceEngine balanceEngine;

    public GroupController(GroupService groupService, BalanceEngine balanceEngine) {
        this.groupService = groupService;
        this.balanceEngine = balanceEngine;
    }

    @GetMapping
    @Operation(summary = "List user groups with net balance")
    public ResponseEntity<List<GroupDto>> getGroups(@RequestParam(required = false) GroupStatus status) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(groupService.getUserGroups(userId, status));
    }

    @PostMapping
    @Operation(summary = "Create a new expense group")
    public ResponseEntity<GroupDto> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        GroupDto created = groupService.createGroup(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/groups/" + created.getId())).body(created);
    }

    @GetMapping("/{groupId:[0-9]+}")
    @Operation(summary = "Get group details with balance and membership check")
    public ResponseEntity<GroupDto> getGroup(@PathVariable Long groupId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(groupService.getGroupDetail(userId, groupId));
    }

    @PutMapping("/{groupId:[0-9]+}")
    @Operation(summary = "Rename or update description (Owner only)")
    public ResponseEntity<GroupDto> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(groupService.updateGroup(userId, groupId, request));
    }

    @PatchMapping("/{groupId:[0-9]+}/status")
    @Operation(summary = "Archive group when all net balances are zero (Owner only)")
    public ResponseEntity<GroupDto> archiveGroup(@PathVariable Long groupId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(groupService.archiveGroup(userId, groupId));
    }

    @GetMapping("/{groupId:[0-9]+}/members")
    @Operation(summary = "List group members with role, status, and net balance")
    public ResponseEntity<List<GroupMemberDto>> getMembers(@PathVariable Long groupId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(groupService.getMembers(userId, groupId));
    }

    @DeleteMapping("/{groupId:[0-9]+}/members/{memberId:[0-9]+}")
    @Operation(summary = "Remove member when net balance is zero (Owner only)")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long memberId) {
        Long userId = SecurityUtils.getCurrentUserId();
        groupService.removeMember(userId, groupId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId:[0-9]+}/leave")
    @Operation(summary = "Leave group when net balance is zero and no pending settlements")
    public ResponseEntity<Void> leaveGroup(@PathVariable Long groupId) {
        Long userId = SecurityUtils.getCurrentUserId();
        groupService.leaveGroup(userId, groupId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupId:[0-9]+}/owner")
    @Operation(summary = "Transfer group ownership to another active member")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable Long groupId,
            @RequestBody Map<String, Long> payload) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long newOwnerId = payload.get("newOwnerId");
        groupService.transferOwnership(userId, groupId, newOwnerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{groupId:[0-9]+}/balances")
    @Operation(summary = "Get paid, owed, settled, and net balances for all members")
    public ResponseEntity<GroupBalancesDto> getBalances(@PathVariable Long groupId) {
        Long userId = SecurityUtils.getCurrentUserId();
        groupService.verifyActiveMember(groupId, userId);
        return ResponseEntity.ok(balanceEngine.computeGroupBalances(groupId));
    }

    @GetMapping({"/{groupId:[0-9]+}/settlement-suggestions", "/{groupId:[0-9]+}/settlements/suggestions"})
    @Operation(summary = "Get minimal set of settlement transfers to clear all dues (at most n-1)")
    public ResponseEntity<List<SettlementSuggestionDto>> getSuggestions(@PathVariable Long groupId) {
        Long userId = SecurityUtils.getCurrentUserId();
        groupService.verifyActiveMember(groupId, userId);
        return ResponseEntity.ok(balanceEngine.computeSettlementSuggestions(groupId));
    }

    @GetMapping("/{groupId:[0-9]+}/history")
    @Operation(summary = "Get unified group activity feed (expenses, settlements, membership)")
    public ResponseEntity<List<GroupActivityDto>> getHistory(
            @PathVariable Long groupId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long memberId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(groupService.getGroupHistory(userId, groupId, type, memberId));
    }
}
