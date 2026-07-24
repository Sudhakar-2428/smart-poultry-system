package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.ChickGrowthRequest;
import com.poultry.backend.dto.ChickGrowthResponse;
import com.poultry.backend.entity.Gender;
import com.poultry.backend.entity.GrowthStage;
import com.poultry.backend.entity.HealthStatus;
import com.poultry.backend.service.ChickGrowthService;
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
@RequestMapping({"/api/v1/chick-growth", "/chick-growth"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chick Growth Tracking", description = "Endpoints for logging daily growth records, height, weight, and general health parameters of growing chicks")
public class ChickGrowthController {

    private final ChickGrowthService growthService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a chick growth record", 
               description = "Log physical traits for a growing chick. Eligible only if chicken belongs to CHICK category and has status BROODER/GROWING. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<ChickGrowthResponse>> createGrowthRecord(@Valid @RequestBody ChickGrowthRequest request) {
        log.info("REST request to log chick growth. Chicken ID: {}", request.getChickenId());
        ChickGrowthResponse response = growthService.createGrowthRecord(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Chick growth record created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get growth record details by ID", description = "Retrieve specific details of a chick growth log record.")
    public ResponseEntity<ApiResponse<ChickGrowthResponse>> getGrowthRecordById(@PathVariable Long id) {
        log.info("REST request to retrieve growth record ID: {}", id);
        ChickGrowthResponse response = growthService.getGrowthRecordById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Growth record retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a growth record", 
               description = "Modify an existing growth log entry. Cannot log multiple logs on the same date for a single chick. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<ChickGrowthResponse>> updateGrowthRecord(
            @PathVariable Long id,
            @Valid @RequestBody ChickGrowthRequest request) {
        log.info("REST request to update growth record ID: {}", id);
        ChickGrowthResponse response = growthService.updateGrowthRecord(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Growth record updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete growth record", description = "Permanently remove growth log by record ID. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<Void>> deleteGrowthRecord(@PathVariable Long id) {
        log.info("REST request to delete growth record ID: {}", id);
        growthService.deleteGrowthRecord(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Growth record deleted successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search growth record history", 
               description = "Search chick growth logs with pagination, filtering options across weight limits, age boundaries, health status or dates.")
    public ResponseEntity<ApiResponse<Page<ChickGrowthResponse>>> searchGrowthRecords(
            @RequestParam(required = false) GrowthStage growthStage,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) HealthStatus healthStatus,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) Double minWeight,
            @RequestParam(required = false) Double maxWeight,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        log.info("REST request to search chick growth records, filters: growthStage={}, healthStatus={}", growthStage, healthStatus);

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<ChickGrowthResponse> results = growthService.searchGrowthRecords(
                growthStage, gender, healthStatus, minAge, maxAge, minWeight, maxWeight, startDate, endDate, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(results, "Growth records search query processed successfully"));
    }
}
