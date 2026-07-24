package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.EggBatchRequest;
import com.poultry.backend.dto.EggBatchResponse;
import com.poultry.backend.dto.EggBatchStatusRequest;
import com.poultry.backend.dto.EggBatchSummaryResponse;
import com.poultry.backend.entity.EggBatchStatus;
import com.poultry.backend.entity.EggPurpose;
import com.poultry.backend.service.EggService;
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
@RequestMapping({"/api/v1/egg-batches", "/egg-batches"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Egg Batch Management", description = "Endpoints for creating and tracking incubating / sale cohorts (batches)")
public class EggBatchController {

    private final EggService eggService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create an egg batch", description = "Initialize a new egg batch for incubation, consumption, or sale. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<EggBatchResponse>> createEggBatch(@Valid @RequestBody EggBatchRequest request) {
        log.info("REST request to build egg batch with code: {}", request.getBatchCode());
        EggBatchResponse response = eggService.createEggBatch(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Egg batch created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get detailed egg batch details", description = "Fetch complete info for a specific batch including dynamic ages, remaining days and success percentage.")
    public ResponseEntity<ApiResponse<EggBatchResponse>> getEggBatchById(@PathVariable Long id) {
        log.info("REST request to view batch ID: {}", id);
        EggBatchResponse response = eggService.getEggBatchById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Egg batch details retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update egg batch details", description = "Modify values for an existing batch record. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<EggBatchResponse>> updateEggBatch(
            @PathVariable Long id,
            @Valid @RequestBody EggBatchRequest request) {
        log.info("REST request to update batch ID: {}", id);
        EggBatchResponse response = eggService.updateEggBatch(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Egg batch details updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update batch status", description = "Perform status transitions and specify actual hatch dates if completed. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<EggBatchResponse>> changeBatchStatus(
            @PathVariable Long id,
            @Valid @RequestBody EggBatchStatusRequest request) {
        log.info("REST request to patch batch status for ID: {} to {}", id, request.getStatus());
        EggBatchResponse response = eggService.changeBatchStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Egg batch status successfully changed"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete an egg batch", description = "Permanently delete an egg batch. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<Void>> deleteEggBatch(@PathVariable Long id) {
        log.info("REST request to delete batch ID: {}", id);
        eggService.deleteEggBatch(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Egg batch deleted successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search and query egg batches", description = "Search egg batches with filters for batch code prefix, status, query purpose, and dates ranges.")
    public ResponseEntity<ApiResponse<Page<EggBatchSummaryResponse>>> searchEggBatches(
            @RequestParam(required = false) String batchCode,
            @RequestParam(required = false) EggBatchStatus status,
            @RequestParam(required = false) EggPurpose purpose,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedHatchDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "batchDate,desc") String sort) {

        log.info("REST request to search batches. Code: {}, status: {}, purpose: {}", batchCode, status, purpose);

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<EggBatchSummaryResponse> results = eggService.searchEggBatches(
                batchCode, status, purpose, expectedHatchDate, startDate, endDate, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(results, "Egg batches query processed successfully"));
    }
}
