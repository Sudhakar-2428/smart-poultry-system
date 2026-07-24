package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.FeedItemRequest;
import com.poultry.backend.dto.FeedItemResponse;
import com.poultry.backend.entity.FeedStatus;
import com.poultry.backend.entity.FeedType;
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
@RequestMapping({"/api/v1/feed-items", "/feed-items"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Feed Item Management", description = "Endpoints for configuring and querying feed stock listings")
public class FeedItemController {

    private final FeedService feedService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Register a new feed item in stock catalog", description = "Creates a feed catalog entry. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<FeedItemResponse>> createFeedItem(@Valid @RequestBody FeedItemRequest request) {
        log.info("REST request to build feed item. Code: {}", request.getFeedCode());
        FeedItemResponse response = feedService.createFeedItem(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Feed item registered successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get feed item by ID", description = "Query specific feed items and remaining stocks.")
    public ResponseEntity<ApiResponse<FeedItemResponse>> getFeedItemById(@PathVariable Long id) {
        log.info("REST request to view feed item: {}", id);
        FeedItemResponse response = feedService.getFeedItemById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Feed item retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update feed details and manual levels", description = "Updates settings or manual stock modifications. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<FeedItemResponse>> updateFeedItem(
            @PathVariable Long id,
            @Valid @RequestBody FeedItemRequest request) {
        log.info("REST request to override feed item ID: {}", id);
        FeedItemResponse response = feedService.updateFeedItem(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Feed item updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Instantly wipe out feed item from catalog", description = "Removes feed record from stock tracking. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<Void>> deleteFeedItem(@PathVariable Long id) {
        log.info("REST request to drop feed item ID: {}", id);
        feedService.deleteFeedItem(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Feed item deleted successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search feed catalogs", description = "Support pagination, sorting, and filtration by feed type, status, or expiry dates.")
    public ResponseEntity<ApiResponse<Page<FeedItemResponse>>> searchFeedItems(
            @RequestParam(required = false) FeedType feedType,
            @RequestParam(required = false) FeedStatus status,
            @RequestParam(required = false) LocalDate expiryDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        log.info("REST request to search feed items");

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<FeedItemResponse> results = feedService.searchFeedItems(feedType, status, expiryDate, pageable);

        return ResponseEntity.ok(ApiResponse.success(results, "Feed items search completed successfully"));
    }
}
