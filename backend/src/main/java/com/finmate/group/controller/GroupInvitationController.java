package com.finmate.group.controller;

import com.finmate.common.util.SecurityUtils;
import com.finmate.group.dto.CreateInvitationRequest;
import com.finmate.group.dto.InvitationDto;
import com.finmate.group.dto.RespondInvitationRequest;
import com.finmate.group.entity.InvitationStatus;
import com.finmate.group.service.GroupInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Group Invitations", description = "Endpoints for managing group invitations")
public class GroupInvitationController {

    private final GroupInvitationService invitationService;

    public GroupInvitationController(GroupInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/groups/{groupId}/invitations")
    @Operation(summary = "Invite registered user to group by email (Owner only)")
    public ResponseEntity<InvitationDto> inviteUser(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateInvitationRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        InvitationDto created = invitationService.inviteUserByEmail(userId, groupId, request);
        return ResponseEntity.created(URI.create("/api/v1/groups/" + groupId + "/invitations/" + created.getId())).body(created);
    }

    @GetMapping("/groups/{groupId}/invitations")
    @Operation(summary = "List pending and historical invitations for group (Owner only)")
    public ResponseEntity<List<InvitationDto>> getGroupInvitations(@PathVariable Long groupId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invitationService.getGroupInvitations(userId, groupId));
    }

    @DeleteMapping("/groups/{groupId}/invitations/{invitationId}")
    @Operation(summary = "Cancel a pending invitation (Owner only)")
    public ResponseEntity<Void> cancelInvitation(
            @PathVariable Long groupId,
            @PathVariable Long invitationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        invitationService.cancelInvitation(userId, groupId, invitationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invitations")
    @Operation(summary = "Get invitations received by authenticated user")
    public ResponseEntity<List<InvitationDto>> getMyInvitations(@RequestParam(required = false) InvitationStatus status) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invitationService.getMyInvitations(userId, status));
    }

    @GetMapping({"/invitations/pending", "/groups/invitations/pending"})
    @Operation(summary = "Get pending invitations received by authenticated user")
    public ResponseEntity<List<InvitationDto>> getPendingInvitations() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invitationService.getMyInvitations(userId, InvitationStatus.PENDING));
    }

    @PatchMapping("/invitations/{invitationId}")
    @Operation(summary = "Accept or reject a received invitation")
    public ResponseEntity<InvitationDto> respondToInvitation(
            @PathVariable Long invitationId,
            @Valid @RequestBody RespondInvitationRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invitationService.respondToInvitation(userId, invitationId, request));
    }
}
