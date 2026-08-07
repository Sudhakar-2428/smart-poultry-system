package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.IncubatorRequest;
import com.poultry.backend.dto.IncubatorResponse;
import com.poultry.backend.dto.IncubatorStatusRequest;
import com.poultry.backend.entity.IncubatorStatus;
import com.poultry.backend.service.HatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping({"/api/v1/incubators", "/incubators"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Incubator Management", description = "Endpoints for scheduling and logging poultry incubator batch cycles")
public class IncubatorController {

    private final HatchingService hatchingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Start incubation", description = "Schedule an egg batch into the incubator. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<IncubatorResponse>> createIncubator(@Valid @RequestBody IncubatorRequest request) {
        log.info("REST request to start incubator batch: {}", request.getBatchCode());
        IncubatorResponse response = hatchingService.createIncubator(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Incubator batch started successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get incubator details by ID", description = "Retrieve specific temperature, humidity and expected hatch dates for a session.")
    public ResponseEntity<ApiResponse<IncubatorResponse>> getIncubatorById(@PathVariable Long id) {
        log.info("REST request to get incubator batch: {}", id);
        IncubatorResponse response = hatchingService.getIncubatorById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Incubator batch details retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update incubator parameters", description = "Modify details of an active/existing incubator batch. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<IncubatorResponse>> updateIncubator(
            @PathVariable Long id,
            @Valid @RequestBody IncubatorRequest request) {
        log.info("REST request to update incubator ID: {}", id);
        IncubatorResponse response = hatchingService.updateIncubator(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Incubator batch updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Change incubator status", description = "Change incubation status lifecycle indicators. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<IncubatorResponse>> changeIncubatorStatus(
            @PathVariable Long id,
            @Valid @RequestBody IncubatorStatusRequest request) {
        log.info("REST request to patch incubator status for ID: {} to {}", id, request.getStatus());
        IncubatorResponse response = hatchingService.changeIncubatorStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Incubator status successfully updated"));
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Hatching Dashboard stats", description = "Retrieve KPI metrics for Hatching dashboard.")
    public ResponseEntity<ApiResponse<com.poultry.backend.dto.HatchingDashboardStats>> getDashboardStats() {
        log.info("REST request to fetch hatching dashboard stats");
        com.poultry.backend.dto.HatchingDashboardStats stats = hatchingService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Hatching stats retrieved successfully"));
    }

    @PostMapping("/{id}/candling")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Record candling check", description = "Log fertile, infertile, and dead embryo counts for a batch.")
    public ResponseEntity<ApiResponse<com.poultry.backend.dto.CandlingRecordDTOs.CandlingRecordResponse>> recordCandling(
            @PathVariable Long id,
            @Valid @RequestBody com.poultry.backend.dto.CandlingRecordDTOs.CandlingRecordRequest request) {
        request.setIncubatorBatchId(id);
        com.poultry.backend.dto.CandlingRecordDTOs.CandlingRecordResponse response = hatchingService.recordCandling(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Candling record logged successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/candling")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get candling history for a batch", description = "Retrieve candling records ordered by day.")
    public ResponseEntity<ApiResponse<java.util.List<com.poultry.backend.dto.CandlingRecordDTOs.CandlingRecordResponse>>> getCandlingRecords(@PathVariable Long id) {
        java.util.List<com.poultry.backend.dto.CandlingRecordDTOs.CandlingRecordResponse> records = hatchingService.getCandlingRecords(id);
        return ResponseEntity.ok(ApiResponse.success(records, "Candling records retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search incubator batches", description = "Search incubator batches with optional code prefix, status filters and start date bounds.")
    public ResponseEntity<ApiResponse<Page<IncubatorResponse>>> searchIncubators(
            @RequestParam(required = false) String batchCode,
            @RequestParam(required = false) IncubatorStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedHatchDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate,desc") String sort) {

        log.info("REST request to search incubator batches. Code: {}, status: {}", batchCode, status);

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<IncubatorResponse> results = hatchingService.searchIncubators(
                batchCode, status, expectedHatchDate, startDate, endDate, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(results, "Incubators query processed successfully"));
    }
}
