package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.EggCollectionDTOs.*;
import com.poultry.backend.service.EggCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/egg-collections")
@RequiredArgsConstructor
public class EggCollectionController {

    private final EggCollectionService eggCollectionService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = eggCollectionService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Egg collection dashboard statistics retrieved successfully."));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<EggCollectionResponse>>> getActiveLayingHens(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EggCollectionResponse> hens = eggCollectionService.getActiveLayingHens(pageable);
        return ResponseEntity.ok(ApiResponse.success(hens, "Active laying hens retrieved successfully."));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EggCollectionResponse>> getCollectionById(@PathVariable Long id) {
        EggCollectionResponse response = eggCollectionService.getCollectionById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Egg collection details retrieved successfully."));
    }

    @GetMapping("/hen/{henId}/profile")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<HenLayingProfileResponse>> getHenLayingProfile(@PathVariable Long henId) {
        HenLayingProfileResponse profile = eggCollectionService.getHenLayingProfile(henId);
        return ResponseEntity.ok(ApiResponse.success(profile, "Hen laying profile retrieved successfully."));
    }

    @PostMapping("/record-today")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EggCollectionResponse>> recordDailyEggs(
            @Valid @RequestBody DailyEggRecordRequest request
    ) {
        log.info("Recording daily eggs for collection ID: {}, eggs: {}", request.getEggCollectionId(), request.getNumberOfEggs());
        EggCollectionResponse response = eggCollectionService.recordDailyEggs(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Daily egg collection recorded successfully."));
    }

    @GetMapping("/eggs")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<EggItemResponse>>> getEggItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String purpose,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EggItemResponse> items = eggCollectionService.getEggItems(category, breed, purpose, pageable);
        return ResponseEntity.ok(ApiResponse.success(items, "Egg items retrieved successfully."));
    }

    @PatchMapping("/eggs/{id}/purpose")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EggItemResponse>> updateEggPurpose(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEggPurposeRequest request
    ) {
        request.setEggId(id);
        EggItemResponse updated = eggCollectionService.updateEggPurpose(request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Egg purpose updated successfully."));
    }

    @PostMapping("/send-to-hatching")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> sendEggsToHatching(
            @Valid @RequestBody SendToHatchingRequest request
    ) {
        int count = eggCollectionService.sendEggsToHatching(request);
        return ResponseEntity.ok(ApiResponse.success(count, count + " eggs successfully transferred to Hatching Module."));
    }
}
