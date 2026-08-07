package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.PurchaseBatchDTOs;
import com.poultry.backend.service.PurchaseBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/purchase-batches", "/purchase-batches"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Purchased Chicken Registration", description = "Endpoints for purchase batch creation, automatic registration number generation (PB01-001), and reports")
public class PurchaseBatchController {

    private final PurchaseBatchService purchaseBatchService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create purchase batch & register chickens", description = "Creates a purchase batch and automatically registers chickens with intelligent code sequence (PB01-001, PB01-002, etc.).")
    public ResponseEntity<ApiResponse<PurchaseBatchDTOs.PurchaseBatchResponse>> createPurchaseBatch(@Valid @RequestBody PurchaseBatchDTOs.CreatePurchaseBatchRequest request) {
        log.info("REST request to create Purchase Batch from Supplier: {}", request.getSupplierName());
        PurchaseBatchDTOs.PurchaseBatchResponse response = purchaseBatchService.createPurchaseBatch(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Purchase batch created and chickens registered successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all purchase batches", description = "Retrieves all purchase batches and their registered chickens.")
    public ResponseEntity<ApiResponse<List<PurchaseBatchDTOs.PurchaseBatchResponse>>> getAllPurchaseBatches() {
        log.info("REST request to list all purchase batches");
        List<PurchaseBatchDTOs.PurchaseBatchResponse> list = purchaseBatchService.getAllPurchaseBatches();
        return ResponseEntity.ok(ApiResponse.success(list, "Purchase batches retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get purchase batch details", description = "Retrieves purchase batch details and registered chickens by ID.")
    public ResponseEntity<ApiResponse<PurchaseBatchDTOs.PurchaseBatchResponse>> getPurchaseBatchById(@PathVariable Long id) {
        log.info("REST request to get purchase batch details for ID: {}", id);
        PurchaseBatchDTOs.PurchaseBatchResponse response = purchaseBatchService.getPurchaseBatchById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase batch details retrieved successfully"));
    }

    @GetMapping("/reports")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get purchased chicken reports", description = "Retrieves purchased chicken reports grouped by purchase batch, supplier, or date.")
    public ResponseEntity<ApiResponse<PurchaseBatchDTOs.PurchasedChickenReportDTO>> getPurchasedChickenReport(
            @RequestParam(defaultValue = "ALL") String reportType,
            @RequestParam(required = false) String supplierName) {

        log.info("REST request to fetch purchased chicken report. Type: {}, Supplier: {}", reportType, supplierName);
        PurchaseBatchDTOs.PurchasedChickenReportDTO report = purchaseBatchService.getPurchasedChickenReport(reportType, supplierName);
        return ResponseEntity.ok(ApiResponse.success(report, "Purchased chicken report generated successfully"));
    }
}
