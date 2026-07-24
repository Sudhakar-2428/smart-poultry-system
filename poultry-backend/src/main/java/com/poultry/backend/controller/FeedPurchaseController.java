package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.FeedPurchaseRequest;
import com.poultry.backend.dto.FeedPurchaseResponse;
import com.poultry.backend.entity.PaymentStatus;
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

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/feed-purchases")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Feed Purchase Registry", description = "Endpoints to record, process, and list feed acquisitions")
public class FeedPurchaseController {

    private final FeedService feedService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Record a feed purchase and credit quantity to stock", 
               description = "Registers feed purchases, updates current stocks, and posts reusable FinanceEvent hooks. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<FeedPurchaseResponse>> recordPurchase(@Valid @RequestBody FeedPurchaseRequest request) {
        log.info("REST request to record feed purchase. Code: {}", request.getPurchaseCode());
        FeedPurchaseResponse response = feedService.recordFeedPurchase(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Feed purchase recorded successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get feed purchase details by ID", description = "Query specific invoice or item quantity on a purchase transaction.")
    public ResponseEntity<ApiResponse<FeedPurchaseResponse>> getPurchaseById(@PathVariable Long id) {
        log.info("REST request to view purchase ID: {}", id);
        FeedPurchaseResponse response = feedService.getPurchaseById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search feed purchase transactions list", 
               description = "Filter purchases list by supplier, payment status, or purchase date with pagination support.")
    public ResponseEntity<ApiResponse<Page<FeedPurchaseResponse>>> searchPurchases(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) LocalDate purchaseDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        log.info("REST request to search feed purchases");

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<FeedPurchaseResponse> results = feedService.searchPurchases(supplierId, paymentStatus, purchaseDate, pageable);

        return ResponseEntity.ok(ApiResponse.success(results, "Purchases search completed successfully"));
    }
}
