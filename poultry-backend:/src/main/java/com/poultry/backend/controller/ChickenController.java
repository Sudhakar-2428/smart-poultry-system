package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.ChickenRequest;
import com.poultry.backend.dto.ChickenResponse;
import com.poultry.backend.dto.ChickenSummaryResponse;
import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.Gender;
import com.poultry.backend.service.ChickenService;
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

@Slf4j
@RestController
@RequestMapping("/chickens")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chicken Management", description = "Endpoints for managing chicken flock records, dynamic query searches, and validation rules")
public class ChickenController {

    private final ChickenService chickenService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Register a new chicken", description = "Add a new chicken to the farm system. Requires ADMIN or MANAGER privileges.")
    public ResponseEntity<ApiResponse<ChickenResponse>> createChicken(@Valid @RequestBody ChickenRequest request) {
        log.info("REST request to register new chicken. Code: {}", request.getChickenCode());
        ChickenResponse response = chickenService.createChicken(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Chicken registered successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get chicken details by ID", description = "Retrieve detailed information for a specific chicken by database ID.")
    public ResponseEntity<ApiResponse<ChickenResponse>> getChickenById(@PathVariable Long id) {
        log.info("REST request to view chicken ID: {}", id);
        ChickenResponse response = chickenService.getChickenById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Chicken details retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update chicken details", description = "Modify values of an existing chicken record. Requires ADMIN or MANAGER privileges.")
    public ResponseEntity<ApiResponse<ChickenResponse>> updateChicken(
            @PathVariable Long id,
            @Valid @RequestBody ChickenRequest request) {
        log.info("REST request to update chicken ID: {}", id);
        ChickenResponse response = chickenService.updateChicken(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Chicken details updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a chicken", description = "Permanently remove a chicken record from the registry database. Cannot delete SOLD or DEAD chickens. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<Void>> deleteChicken(@PathVariable Long id) {
        log.info("REST request to delete chicken ID: {}", id);
        chickenService.deleteChicken(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Chicken deleted successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search and filter chickens", description = "Retrieve a paginated, sorted list of chickens dynamically filtered by breed, gender, category, status, weight and dynamic age limits.")
    public ResponseEntity<ApiResponse<Page<ChickenSummaryResponse>>> searchChickens(
            @RequestParam(required = false) Breed breed,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) ChickenCategory category,
            @RequestParam(required = false) ChickenStatus status,
            @RequestParam(required = false) Integer minAgeDays,
            @RequestParam(required = false) Integer maxAgeDays,
            @RequestParam(required = false) Double minWeight,
            @RequestParam(required = false) Double maxWeight,
            @RequestParam(required = false) String chickenCode,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        log.info("REST request to search chickens with status: {}, category: {}, age range: [{}-{}]", status, category, minAgeDays, maxAgeDays);

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<ChickenSummaryResponse> results = chickenService.searchChickens(
                breed, gender, category, status, minAgeDays, maxAgeDays, minWeight, maxWeight, chickenCode, name, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(results, "Chickens search query processed successfully"));
    }
}
