package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.HatchingReportDTOs;
import com.poultry.backend.service.HatchingReportService;
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
@RequestMapping({"/api/v1/hatching-reports", "/hatching-reports"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Hatching Report Management", description = "Endpoints for retrieving automated hatching reports for completed batches")
public class HatchingReportController {

    private final HatchingReportService hatchingReportService;

    @GetMapping("/batch/{incubatorBatchId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Hatching Report by Batch ID", description = "Retrieve comprehensive hatching report including parent metadata, candling summary, and performance math.")
    public ResponseEntity<ApiResponse<HatchingReportDTOs.HatchingReportResponse>> getReportByBatchId(@PathVariable Long incubatorBatchId) {
        log.info("REST request to fetch Hatching Report for batch ID: {}", incubatorBatchId);
        HatchingReportDTOs.HatchingReportResponse response = hatchingReportService.getReportByBatchId(incubatorBatchId);
        return ResponseEntity.ok(ApiResponse.success(response, "Hatching report retrieved successfully"));
    }

    @PostMapping("/batch/{incubatorBatchId}/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Generate/Regenerate Hatching Report", description = "Generate or refresh Hatching Report for a batch.")
    public ResponseEntity<ApiResponse<HatchingReportDTOs.HatchingReportResponse>> generateHatchingReport(@PathVariable Long incubatorBatchId) {
        log.info("REST request to generate Hatching Report for batch ID: {}", incubatorBatchId);
        HatchingReportDTOs.HatchingReportResponse response = hatchingReportService.generateHatchingReport(incubatorBatchId);
        return ResponseEntity.ok(ApiResponse.success(response, "Hatching report generated successfully"));
    }
}
