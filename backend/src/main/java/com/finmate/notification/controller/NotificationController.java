package com.finmate.notification.controller;

import com.finmate.common.util.SecurityUtils;
import com.finmate.notification.dto.NotificationDto;
import com.finmate.notification.dto.UnreadCountDto;
import com.finmate.notification.entity.NotificationType;
import com.finmate.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Endpoints for viewing and managing in-app notifications and alerts")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List user notifications with optional unread filter and type filter")
    public ResponseEntity<Page<NotificationDto>> getNotifications(
            @RequestParam(name = "unread", required = false) Boolean unread,
            @RequestParam(name = "unreadOnly", required = false) Boolean unreadOnly,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean filterUnread = Boolean.TRUE.equals(unread) || Boolean.TRUE.equals(unreadOnly);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, filterUnread, type, page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread notifications for badge display")
    public ResponseEntity<UnreadCountDto> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(new UnreadCountDto(notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(notificationService.markRead(userId, id));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.deleteNotification(userId, id);
        return ResponseEntity.noContent().build();
    }
}
