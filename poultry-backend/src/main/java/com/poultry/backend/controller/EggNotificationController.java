package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.EggNotificationDTOs;
import com.poultry.backend.service.EggNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/egg-notifications", "/egg-notifications"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Smart Egg Collection Notification & Escalation", description = "Endpoints for 08:00 AM collection notifications, YES/NO/reschedule actions, 06:00 PM escalations, and reports")
public class EggNotificationController {

    private final EggNotificationService eggNotificationService;

    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get active pending egg notifications", description = "Retrieves active pending egg collection notifications for floating popups and notification center.")
    public ResponseEntity<ApiResponse<List<EggNotificationDTOs.EggNotificationResponse>>> getActivePendingNotifications() {
        log.info("REST request to fetch active pending egg notifications");
        List<EggNotificationDTOs.EggNotificationResponse> list = eggNotificationService.getActivePendingNotifications();
        return ResponseEntity.ok(ApiResponse.success(list, "Active pending egg notifications retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List egg notifications by status", description = "Retrieves egg collection notifications by status filter (PENDING, COMPLETED, NO_EGG, ESCALATED, OVERDUE).")
    public ResponseEntity<ApiResponse<List<EggNotificationDTOs.EggNotificationResponse>>> getNotificationsByStatus(
            @RequestParam(required = false) String status) {
        log.info("REST request to fetch egg notifications for status: {}", status);
        List<EggNotificationDTOs.EggNotificationResponse> list = eggNotificationService.getNotificationsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(list, "Egg notifications retrieved successfully"));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirm YES egg laying response", description = "Confirms healthy/broken/damaged egg collection for a hen and updates counts & timeline.")
    public ResponseEntity<ApiResponse<EggNotificationDTOs.EggNotificationResponse>> confirmEggCollection(
            @PathVariable Long id,
            @Valid @RequestBody EggNotificationDTOs.ConfirmEggCollectionRequest request,
            Authentication authentication) {
        String currentUser = authentication != null ? authentication.getName() : "User";
        log.info("REST request YES response for notification ID: {} by user: {}", id, currentUser);
        EggNotificationDTOs.EggNotificationResponse response = eggNotificationService.confirmEggCollection(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Egg collection confirmed successfully"));
    }

    @PostMapping("/{id}/no-egg")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Record NO egg response with reason", description = "Stores reason (No Egg Today, Brooding, Sick, Stress, Low Feed Intake, Other) for no egg laid.")
    public ResponseEntity<ApiResponse<EggNotificationDTOs.EggNotificationResponse>> recordNoEgg(
            @PathVariable Long id,
            @Valid @RequestBody EggNotificationDTOs.NoEggReasonRequest request,
            Authentication authentication) {
        String currentUser = authentication != null ? authentication.getName() : "User";
        log.info("REST request NO response for notification ID: {} by user: {}", id, currentUser);
        EggNotificationDTOs.EggNotificationResponse response = eggNotificationService.recordNoEgg(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "No-egg response recorded successfully"));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reschedule STILL NOT NOW notification", description = "Reschedules reminder for selected duration (30m, 1h, 2h, 3h, 4h, 5h).")
    public ResponseEntity<ApiResponse<EggNotificationDTOs.EggNotificationResponse>> rescheduleNotification(
            @PathVariable Long id,
            @Valid @RequestBody EggNotificationDTOs.RescheduleNotificationRequest request,
            Authentication authentication) {
        String currentUser = authentication != null ? authentication.getName() : "User";
        log.info("REST request STILL NOT NOW reschedule for notification ID: {} by user: {}", id, currentUser);
        EggNotificationDTOs.EggNotificationResponse response = eggNotificationService.rescheduleNotification(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification rescheduled successfully"));
    }

    @PostMapping("/trigger-08am")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Trigger 08:00 AM notifications job", description = "Manually trigger 08:00 AM egg collection notifications job.")
    public ResponseEntity<ApiResponse<String>> trigger08AMNotifications() {
        log.info("REST request to manually trigger 08:00 AM notifications job");
        eggNotificationService.triggerDaily08AMNotifications();
        return ResponseEntity.ok(ApiResponse.success("08:00 AM notifications job triggered successfully", "Triggered"));
    }

    @PostMapping("/trigger-06pm-escalation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Trigger 06:00 PM escalation job", description = "Manually trigger 06:00 PM escalation job.")
    public ResponseEntity<ApiResponse<String>> trigger06PMEscalation() {
        log.info("REST request to manually trigger 06:00 PM escalation job");
        eggNotificationService.process06PMEscalation();
        return ResponseEntity.ok(ApiResponse.success("06:00 PM escalation job triggered successfully", "Triggered"));
    }

    @GetMapping("/reports")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get egg collection notification reports", description = "Retrieves egg collection notification reports and statistics.")
    public ResponseEntity<ApiResponse<EggNotificationDTOs.EggNotificationReportDTO>> getNotificationReport(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("REST request to fetch egg notification report");
        EggNotificationDTOs.EggNotificationReportDTO report = eggNotificationService.getNotificationReport(status, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report, "Egg notification report generated successfully"));
    }
}
