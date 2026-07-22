package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.BrooderResponse;
import com.poultry.backend.entity.BrooderStatus;
import com.poultry.backend.service.HatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/brooders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Brooder Management", description = "Endpoints for monitoring growing chicks inside heating chambers (brooders)")
public class BrooderController {

    private final HatchingService hatchingService;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get brooder batch by ID", description = "Retrieve locations, expected end dates, and active durations for a brooder cohort.")
    public ResponseEntity<ApiResponse<BrooderResponse>> getBrooderById(@PathVariable Long id) {
        log.info("REST request to view brooder batch ID: {}", id);
        BrooderResponse response = hatchingService.getBrooderById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Brooder batch details retrieved successfully"));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Complete brooder cycle", description = "Mark brooder batch cohort cycle as completed. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<BrooderResponse>> completeBrooder(@PathVariable Long id) {
        log.info("REST request to complete brooder batch ID: {}", id);
        BrooderResponse response = hatchingService.completeBrooder(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Brooder batch completed successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search brooder batches", description = "Search brooders with optional status filters and creation date bounds.")
    public ResponseEntity<ApiResponse<Page<BrooderResponse>>> searchBrooders(
            @RequestParam(required = false) String brooderCode,
            @RequestParam(required = false) BrooderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate,desc") String sort) {

        log.info("REST request to search brooders. Code: {}, status: {}", brooderCode, status);

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<BrooderResponse> results = hatchingService.searchBrooders(
                brooderCode, status, startDate, endDate, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(results, "Brooders query processed successfully"));
    }
}
