package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.*;
import com.poultry.backend.entity.PaymentStatus;
import com.poultry.backend.entity.SaleType;
import com.poultry.backend.entity.SalesOrderStatus;
import com.poultry.backend.service.SalesService;
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
@RequestMapping({"/api/v1/sales-orders", "/sales-orders"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Sales Order Management", description = "Endpoints for managing sales, invoices, items, and billing checkout workflows")
public class SalesOrderController {

    private final SalesService salesService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Log a new sales order invoice", description = "Creates a sales invoice. Validates inventory locks. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> createSalesOrder(@Valid @RequestBody SalesOrderRequest request) {
        log.info("REST request to capture sales order. Number: {}", request.getOrderNumber());
        SalesOrderResponse response = salesService.createSalesOrder(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Sales order captured successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER')")
    @Operation(summary = "Get sales order invoice by ID", description = "Retrieves full invoice details, prices, tax details, and order items.")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> getSalesOrderById(@PathVariable Long id) {
        log.info("REST request to view sales order ID: {}", id);
        SalesOrderResponse response = salesService.getSalesOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales order retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Modify sales order details", description = "Edit draft/confirmed sales invoices. Restored/re-locked inventories. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> updateSalesOrder(
            @PathVariable Long id,
            @Valid @RequestBody SalesOrderRequest request) {
        log.info("REST request to update sales order ID: {}", id);
        SalesOrderResponse response = salesService.updateSalesOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales order updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Transition sales order status", description = "Progress status between DRAFT, CONFIRMED, COMPLETED, or CANCELLED. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody SalesOrderStatusRequest request) {
        log.info("REST request to patch sales order status ID: {} to {}", id, request.getStatus());
        SalesOrderResponse response = salesService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales order status updated successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WORKER')")
    @Operation(summary = "Search sales orders catalog", description = "Query orders by buyers, status, payment state, birds/egg items reference with pagination.")
    public ResponseEntity<ApiResponse<Page<SalesOrderSummaryResponse>>> searchSalesOrders(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) SaleType saleType,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) SalesOrderStatus orderStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDate,
            @RequestParam(required = false) Long chickenId,
            @RequestParam(required = false) Long eggBatchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        log.info("REST request to search sales orders");

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<SalesOrderSummaryResponse> results = salesService.searchSalesOrders(
                customerId, saleType, paymentStatus, orderStatus, orderDate, chickenId, eggBatchId, startDate, endDate, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(results, "Sales orders search completed successfully"));
    }
}
