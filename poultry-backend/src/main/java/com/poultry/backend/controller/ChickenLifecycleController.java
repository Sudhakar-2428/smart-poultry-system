package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.AdultTransitionRequest;
import com.poultry.backend.dto.ChickenResponse;
import com.poultry.backend.dto.GenderUpdateRequest;
import com.poultry.backend.service.ChickGrowthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/chickens", "/chickens"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chicken Lifecycle", description = "Endpoints for chicken gender updates and adult transition processes")
public class ChickenLifecycleController {

    private final ChickGrowthService growthService;

    @PatchMapping("/{id}/gender")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Identify or update chicken gender", 
               description = "Allows ADMIN or MANAGER roles to update chicken gender. Gender cannot be changed after the bird reaches ADULT status unless performed by an ADMIN.")
    public ResponseEntity<ApiResponse<ChickenResponse>> updateChickenGender(
            @PathVariable Long id,
            @Valid @RequestBody GenderUpdateRequest request) {
        log.info("REST request to update chicken gender. ID: {}, gender: {}", id, request.getGender());
        ChickenResponse response = growthService.updateChickenGender(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Chicken gender updated successfully"));
    }

    @PatchMapping("/{id}/adult-transition")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Complete adult transition for a chicken", 
               description = "Transition a chick to an adult bird. Changes Category from CHICK to BROILER/LAYER/BREEDER and Status from GROWING to ACTIVE. Requires ADMIN or MANAGER.")
    public ResponseEntity<ApiResponse<ChickenResponse>> completeAdultTransition(
            @PathVariable Long id,
            @Valid @RequestBody AdultTransitionRequest request) {
        log.info("REST request to complete adult transition for Chicken ID: {}", id);
        ChickenResponse response = growthService.completeAdultTransition(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Chicken transitioned to adult status successfully"));
    }
}
