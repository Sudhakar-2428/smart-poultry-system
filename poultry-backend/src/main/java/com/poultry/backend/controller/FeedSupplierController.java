package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.FeedSupplierRequest;
import com.poultry.backend.dto.FeedSupplierResponse;
import com.poultry.backend.entity.SupplierStatus;
import com.poultry.backend.service.FeedService;
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
@RequestMapping({"/api/v1/feed-suppliers", "/feed-suppliers"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Feed Supplier Management", description = "Endpoints for registering and querying feed vendor credentials")
public class FeedSupplierController {

    private final FeedService feedService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Register a new feed supplier profile", description = "Stores key vendor contacts. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<FeedSupplierResponse>> registerSupplier(@Valid @RequestBody FeedSupplierRequest request) {
        log.info("REST request to register feed supplier. Code: {}", request.getSupplierCode());
        FeedSupplierResponse response = feedService.registerSupplier(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Supplier registered successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get feed supplier profile by ID", description = "Query detailed address and status for a supplier.")
    public ResponseEntity<ApiResponse<FeedSupplierResponse>> getSupplierById(@PathVariable Long id) {
        log.info("REST request to view supplier ID: {}", id);
        FeedSupplierResponse response = feedService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Supplier retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update supplier details", description = "Modifies contact info, address, or status. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<FeedSupplierResponse>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody FeedSupplierRequest request) {
        log.info("REST request to update supplier ID: {}", id);
        FeedSupplierResponse response = feedService.updateSupplier(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Supplier updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete supplier file", description = "Removes supplier profile from archives. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Long id) {
        log.info("REST request to delete supplier: {}", id);
        feedService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Supplier deleted successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search suppliers", description = "Retrieve list of suppliers page-by-page, sorted and filtered by active status.")
    public ResponseEntity<ApiResponse<Page<FeedSupplierResponse>>> searchSuppliers(
            @RequestParam(required = false) SupplierStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        log.info("REST request to search suppliers");

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<FeedSupplierResponse> results = feedService.searchSuppliers(status, pageable);

        return ResponseEntity.ok(ApiResponse.success(results, "Suppliers search completed successfully"));
    }
}
