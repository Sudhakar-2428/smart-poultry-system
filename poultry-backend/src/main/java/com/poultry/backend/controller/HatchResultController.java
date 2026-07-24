package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.HatchResultRequest;
import com.poultry.backend.dto.HatchResultResponse;
import com.poultry.backend.service.HatchingService;
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
@RequestMapping("/hatch-results")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Hatch Result Management", description = "Endpoints for registering hatch ratios and automatically listing baby chicks")
public class HatchResultController {

    private final HatchingService hatchingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Log a hatch result", description = "Record success counts for fertile, hatched and unhatched eggs. Triggers automatic chick and brooder registrations. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<HatchResultResponse>> saveHatchResult(@Valid @RequestBody HatchResultRequest request) {
        log.info("REST request to capture hatch result for incubator ID: {}", request.getIncubatorBatchId());
        HatchResultResponse response = hatchingService.saveHatchResult(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Hatch result recorded and chicks register processed"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get hatch result by ID", description = "Retrieve specific counts, dead embryos, and success rates for a logged hatch outcome.")
    public ResponseEntity<ApiResponse<HatchResultResponse>> getHatchResultById(@PathVariable Long id) {
        log.info("REST request to view hatch result details by ID: {}", id);
        HatchResultResponse response = hatchingService.getHatchResultById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Hatch result details retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search hatch records log", description = "Search history of logged hatch outcomes within optional date bounds.")
    public ResponseEntity<ApiResponse<Page<HatchResultResponse>>> searchHatchResults(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "recordedDate,desc") String sort) {

        log.info("REST request to search hatch results between [{} - {}]", startDate, endDate);

        String[] sortParts = sort.split(",");
        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending()
                : Sort.by(sortParts[0]).descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<HatchResultResponse> results = hatchingService.searchHatchResults(startDate, endDate, pageable);

        return ResponseEntity.ok(ApiResponse.success(results, "Hatch results query processed successfully"));
    }
}
