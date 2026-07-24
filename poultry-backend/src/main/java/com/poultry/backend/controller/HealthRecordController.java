package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.*;
import com.poultry.backend.entity.HealthStatus;
import com.poultry.backend.entity.HealthType;
import com.poultry.backend.service.HealthRecordService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/health-records")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Health Records Management", description = "Endpoints for logged treatments, vaccinations, and mortality entries")
public class HealthRecordController {

    private final HealthRecordService healthService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VETERINARIAN')")
    @Operation(summary = "Log a health checkup or treatment record", 
               description = "Records details for checkups, diseases, surgeries, vaccinations, or deworming programs. Requires ADMIN/MANAGER/VETERINARIAN.")
    public ResponseEntity<ApiResponse<HealthRecordResponse>> createRecord(@Valid @RequestBody HealthRecordRequest request) {
        log.info("REST request to register health record. Code: {}", request.getRecordCode());
        HealthRecordResponse response = healthService.createHealthRecord(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Health record registered successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get health record by ID", description = "Retrieve specific details and logs for a health record entry.")
    public ResponseEntity<ApiResponse<HealthRecordResponse>> getRecordById(@PathVariable Long id) {
        log.info("REST request to view health record ID: {}", id);
        HealthRecordResponse response = healthService.getHealthRecordById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Health record retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VETERINARIAN')")
    @Operation(summary = "Update health record details", 
               description = "Modify diagnosis, treatments, medicines, vaccination due dates, and outcomes. Requires ADMIN/MANAGER/VETERINARIAN.")
    public ResponseEntity<ApiResponse<HealthRecordResponse>> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody HealthRecordRequest request) {
        log.info("REST request to update health record ID: {}", id);
        HealthRecordResponse response = healthService.updateHealthRecord(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Health record updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VETERINARIAN')")
    @Operation(summary = "Delete health record entry", 
               description = "Permanently removes a health record log from the system. Requires ADMIN/MANAGER/VETERINARIAN.")
    public ResponseEntity<ApiResponse<Void>> deleteRecord(@PathVariable Long id) {
        log.info("REST request to delete health record ID: {}", id);
        healthService.deleteHealthRecord(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Health record deleted successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search health records catalog", 
               description = "Filter records by chicken, health types (vaccination, disease, etc), date range, status, or mortality with dynamic pagination and sorting.")
    public ResponseEntity<ApiResponse<Page<HealthRecordSummaryResponse>>> searchRecords(
            @RequestParam(required = false) Long chickenId,
            @RequestParam(required = false) HealthType healthType,
            @RequestParam(required = false) HealthStatus healthStatus,
            @RequestParam(required = false) String diseaseName,
            @RequestParam(required = false) String vaccinationName,
            @RequestParam(required = false) String veterinarian,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Boolean mortality,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        log.info("REST request to search health records");

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<HealthRecordSummaryResponse> results = healthService.searchHealthRecords(
                chickenId, healthType, healthStatus, diseaseName, vaccinationName, veterinarian, startDate, endDate, mortality, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(results, "Health records search completed successfully"));
    }
}
