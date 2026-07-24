package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.EggRecordRequest;
import com.poultry.backend.dto.EggRecordResponse;
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
@RequestMapping({"/api/v1/egg-records", "/egg-records"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Egg Record Management", description = "Endpoints for recording and auditing laying logs for female hens on a daily basis")
public class EggRecordController {

    private final EggService eggService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Record daily laid eggs", description = "Lodge egg laying numbers for a specific female hen. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<EggRecordResponse>> recordDailyEggs(@Valid @RequestBody EggRecordRequest request) {
        log.info("REST request to record daily eggs for hen ID: {}", request.getHenId());
        EggRecordResponse response = eggService.recordDailyEggs(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Daily egg record registered successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get daily egg record by ID", description = "Retrieve specific details of a previously submitted daily egg record.")
    public ResponseEntity<ApiResponse<EggRecordResponse>> getEggRecordById(@PathVariable Long id) {
        log.info("REST request to get egg record ID: {}", id);
        EggRecordResponse response = eggService.getEggRecordById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Egg record details retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update an egg record", description = "Modify details of an egg record. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<EggRecordResponse>> updateEggRecord(
            @PathVariable Long id,
            @Valid @RequestBody EggRecordRequest request) {
        log.info("REST request to update egg record ID: {}", id);
        EggRecordResponse response = eggService.updateEggRecord(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Egg record updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete an egg record", description = "Remove an egg record from logs. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<Void>> deleteEggRecord(@PathVariable Long id) {
        log.info("REST request to delete egg record ID: {}", id);
        eggService.deleteEggRecord(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Egg record deleted successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search and filter daily egg records", description = "Search daily egg records with optional filters for hen source and date range bounds.")
    public ResponseEntity<ApiResponse<Page<EggRecordResponse>>> searchEggRecords(
            @RequestParam(required = false) Long henId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "recordDate,desc") String sort) {

        log.info("REST request to search egg records. Hen ID: {}, range: [{} to {}]", henId, startDate, endDate);

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<EggRecordResponse> results = eggService.searchEggRecords(henId, startDate, endDate, pageable);

        return ResponseEntity.ok(ApiResponse.success(results, "Egg records query processed successfully"));
    }
}
