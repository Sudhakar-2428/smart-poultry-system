package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.NotificationResponse;
import com.poultry.backend.entity.NotificationType;
import com.poultry.backend.entity.RecipientRole;
import com.poultry.backend.entity.Severity;
import com.poultry.backend.entity.SourceModule;
import com.poultry.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notification Center", description = "Endpoints for viewing, searching, acknowledging, and archiving notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER', 'VETERINARIAN')")
    @Operation(summary = "Search and filter notifications",
               description = "Retrieves a paginated list of notifications matching user role boundaries and custom filters.")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> searchNotifications(
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) SourceModule sourceModule,
            @RequestParam(required = false) RecipientRole recipientRole,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Boolean isArchived,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable
    ) {
        log.info("REST request to search notifications with filters");
        Page<NotificationResponse> page = notificationService.searchNotifications(
                type, severity, sourceModule, recipientRole, isRead, isArchived, startDate, endDate, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(page, "Notifications search results retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER', 'VETERINARIAN')")
    @Operation(summary = "Get notification by ID",
               description = "Retrieve details for a specific notification. Enforces user role boundary access rules.")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(@PathVariable Long id) {
        log.info("REST request to view notification ID: {}", id);
        NotificationResponse response = notificationService.getNotificationById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification retrieved successfully"));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER', 'VETERINARIAN')")
    @Operation(summary = "Mark a notification as read",
               description = "Marks a specific notification as read and records the read time. Archived notifications cannot be marked read.")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Long id) {
        log.info("REST request to mark notification read: {}", id);
        NotificationResponse response = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification marked as read successfully"));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER', 'VETERINARIAN')")
    @Operation(summary = "Archive an active notification",
               description = "Archives a specific notification. Already archived notifications cannot be re-archived.")
    public ResponseEntity<ApiResponse<NotificationResponse>> archiveNotification(@PathVariable Long id) {
        log.info("REST request to archive notification ID: {}", id);
        NotificationResponse response = notificationService.archiveNotification(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification archived successfully"));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER', 'VETERINARIAN')")
    @Operation(summary = "Bulk mark active notifications as read",
               description = "Marks all unarchived, active notifications intended for the current user's role as read in bulk.")
    public ResponseEntity<ApiResponse<Integer>> readAll() {
        log.info("REST request to bulk mark notifications as read");
        int count = notificationService.bulkRead();
        return ResponseEntity.ok(ApiResponse.success(count, "All active notifications marked as read successfully: " + count));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER', 'VETERINARIAN')")
    @Operation(summary = "Get unread notifications count",
               description = "Retrieves the total count of unread, active notifications intended for the current user's role.")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        log.info("REST request to query unread notifications count");
        long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.success(count, "Count computed successfully"));
    }
}
