package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.ChickRegistrationDTOs;
import com.poultry.backend.service.ChickRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/chick-registration", "/chick-registration"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Automatic Chick Registration", description = "Endpoints for automatic registration of farm-born chicks, intelligent code generation, parent stats, and reports")
public class ChickRegistrationController {

    private final ChickRegistrationService chickRegistrationService;

    @PostMapping("/hatch-batch/{incubatorBatchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Process automatic chick registration", description = "Automatically create chick records and intelligent IDs for all healthy chicks in a hatch batch.")
    public ResponseEntity<ApiResponse<ChickRegistrationDTOs.ChickRegistrationSummaryResponse>> registerChicksForHatchBatch(@PathVariable Long incubatorBatchId) {
        log.info("REST request to process automatic chick registration for hatch batch ID: {}", incubatorBatchId);
        ChickRegistrationDTOs.ChickRegistrationSummaryResponse response = chickRegistrationService.registerChicksForHatchBatch(incubatorBatchId);
        return ResponseEntity.ok(ApiResponse.success(response, "Chick registration processed successfully"));
    }

    @GetMapping("/parents/{chickenId}/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get parent production statistics", description = "Retrieve total hatch batches, total chicks produced, and partner hens for a parent hen/rooster.")
    public ResponseEntity<ApiResponse<ChickRegistrationDTOs.ParentChickStatsResponse>> getParentChickStats(@PathVariable Long chickenId) {
        log.info("REST request to fetch parent production stats for chicken ID: {}", chickenId);
        ChickRegistrationDTOs.ParentChickStatsResponse response = chickRegistrationService.getParentChickStats(chickenId);
        return ResponseEntity.ok(ApiResponse.success(response, "Parent statistics retrieved successfully"));
    }

    @GetMapping("/reports")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get chick registration reports", description = "Retrieve chick reports grouped by mother, father, hatch batch, or overall date.")
    public ResponseEntity<ApiResponse<ChickRegistrationDTOs.ChickReportDTO>> getChickReport(
            @RequestParam(defaultValue = "ALL") String reportType,
            @RequestParam(required = false) Long filterId) {

        log.info("REST request to fetch chick report. Type: {}, FilterID: {}", reportType, filterId);
        ChickRegistrationDTOs.ChickReportDTO report = chickRegistrationService.getChickReport(reportType, filterId);
        return ResponseEntity.ok(ApiResponse.success(report, "Chick report generated successfully"));
    }
}
