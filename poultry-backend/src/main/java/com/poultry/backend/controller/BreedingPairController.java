package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.*;
import com.poultry.backend.entity.BreedingPurpose;
import com.poultry.backend.entity.PairStatus;
import com.poultry.backend.service.BreedingPairService;
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
@RequestMapping("/pairs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Breeding Pairs Management", description = "Endpoints for scheduling breeding pairs, tracking duration, matching male-female laying cohorts")
public class BreedingPairController {

    private final BreedingPairService pairService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Register a breeding pair", 
               description = "Arrange a male and female chicken under a pairing code. Validates status ACTIVE, gender match, category eligibilities. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<BreedingPairResponse>> createPair(@Valid @RequestBody BreedingPairRequest request) {
        log.info("REST request to register breeding pair. Code: {}", request.getPairCode());
        BreedingPairResponse response = pairService.createPair(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Breeding pair registered successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get breeding pair by ID", description = "Retrieve specific details and status updates of a breeding pair record.")
    public ResponseEntity<ApiResponse<BreedingPairResponse>> getPairById(@PathVariable Long id) {
        log.info("REST request to view breeding pair ID: {}", id);
        BreedingPairResponse response = pairService.getPairById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Breeding pair retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update breeding pair details", 
               description = "Modify schedules, purpose, remarks, chickens. Updates matching pairId parameters in real-time. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<BreedingPairResponse>> updatePair(
            @PathVariable Long id,
            @Valid @RequestBody BreedingPairRequest request) {
        log.info("REST request to update breeding pair ID: {}", id);
        BreedingPairResponse response = pairService.updatePair(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Breeding pair updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Patch update breeding pair status", 
               description = "Transition pair statuses. Clears linked pairId variables if status moves to COMPLETED or CANCELLED. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<BreedingPairResponse>> updatePairStatus(
            @PathVariable Long id,
            @Valid @RequestBody PairStatusUpdateRequest request) {
        log.info("REST request to update breeding pair status ID: {} to {}", id, request.getStatus());
        BreedingPairResponse response = pairService.updatePairStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Breeding pair status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete breeding pair record", 
               description = "Removes pairing entry from DB. Resets child pairId mappings on chickens. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<Void>> deletePair(@PathVariable Long id) {
        log.info("REST request to delete breeding pair ID: {}", id);
        pairService.deletePair(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Breeding pair deleted successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search breeding pair catalog", 
               description = "Query pairs with pagination, sorting, matching codes, or status criteria.")
    public ResponseEntity<ApiResponse<Page<BreedingPairSummaryResponse>>> searchPairs(
            @RequestParam(required = false) Long maleChickenId,
            @RequestParam(required = false) Long femaleChickenId,
            @RequestParam(required = false) PairStatus status,
            @RequestParam(required = false) BreedingPurpose purpose,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        log.info("REST request to search breeding pairs");

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<BreedingPairSummaryResponse> results = pairService.searchPairs(
                maleChickenId, femaleChickenId, status, purpose, startDate, endDate, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(results, "Pairs search query processed successfully"));
    }
}
