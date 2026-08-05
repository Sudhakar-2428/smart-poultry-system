package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.EggCollectionDTOs.EggCollectionResponse;
import com.poultry.backend.dto.EggLayingDTOs.*;
import com.poultry.backend.service.EggLayingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/egg-laying")
@RequiredArgsConstructor
public class EggLayingController {

    private final EggLayingService eggLayingService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EggLayingDashboardStats>> getDashboardStats() {
        EggLayingDashboardStats stats = eggLayingService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Egg laying dashboard statistics retrieved successfully."));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<EggLayingItemResponse>>> getActiveLayingHens(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EggLayingItemResponse> hens = eggLayingService.getActiveLayingHens(pageable);
        return ResponseEntity.ok(ApiResponse.success(hens, "Active egg laying monitoring records retrieved successfully."));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<EggLayingHistoryResponse>>> getLayingHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EggLayingHistoryResponse> history = eggLayingService.getLayingHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success(history, "Egg laying history retrieved successfully."));
    }

    @PostMapping("/{pairId}/start-collection")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EggCollectionResponse>> startEggCollection(@PathVariable Long pairId) {
        log.info("Starting egg collection for pair ID: {}", pairId);
        EggCollectionResponse response = eggLayingService.startEggCollection(pairId);
        return ResponseEntity.ok(ApiResponse.success(response, "Hen successfully transferred to Egg Collection module."));
    }
}
