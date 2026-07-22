package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.FeedConsumptionRequest;
import com.poultry.backend.dto.FeedConsumptionResponse;
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
@RequestMapping("/feed-consumption")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Feed Allocation & Consumption", description = "Endpoints to record and query daily flock/brooder feeding records")
public class FeedConsumptionController {

    private final FeedService feedService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Allocate feed consumption to flock or brooder batch", 
               description = "Registers feed consumption, updates current stock, and flags alerts if stocks cross limits. Requires ADMIN/MANAGER.")
    public ResponseEntity<ApiResponse<FeedConsumptionResponse>> recordConsumption(@Valid @RequestBody FeedConsumptionRequest request) {
        log.info("REST request to record feed consumption. Feed ID: {}", request.getFeedItemId());
        FeedConsumptionResponse response = feedService.recordFeedConsumption(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Feed consumption recorded successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get feed consumption record details by ID", description = "Query specific historical allocation ticket.")
    public ResponseEntity<ApiResponse<FeedConsumptionResponse>> getConsumptionById(@PathVariable Long id) {
        log.info("REST request to view feed consumption ID: {}", id);
        FeedConsumptionResponse response = feedService.getConsumptionById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Consumption record retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search feed allocation logs", 
               description = "Filter consumption logs by feed item, chicken ID, brooder batch, or specific date with pagination support.")
    public ResponseEntity<ApiResponse<Page<FeedConsumptionResponse>>> searchConsumptions(
            @RequestParam(required = false) Long feedItemId,
            @RequestParam(required = false) Long chickenId,
            @RequestParam(required = false) Long brooderBatchId,
            @RequestParam(required = false) LocalDate consumptionDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        log.info("REST request to search feed consumptions");

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<FeedConsumptionResponse> results = feedService.searchConsumptions(feedItemId, chickenId, brooderBatchId, consumptionDate, pageable);

        return ResponseEntity.ok(ApiResponse.success(results, "Consumption logs search completed successfully"));
    }
}
