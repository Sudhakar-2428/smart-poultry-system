package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.DeleteFarmRequest;
import com.poultry.backend.dto.DeleteFarmResponse;
import com.poultry.backend.dto.FarmDeleteCheckResponse;
import com.poultry.backend.service.DeleteFarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/farms/{farmId}", "/farms/{farmId}"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Delete Farm Management", description = "Endpoints for checking eligibility and permanently deleting a farm and all related records")
public class DeleteFarmController {

    private final DeleteFarmService deleteFarmService;

    @GetMapping("/delete-check")
    @Operation(summary = "Check Delete Farm Eligibility", description = "Validates if the caller is the PRIMARY_OWNER and whether active workers are still connected to the farm")
    public ResponseEntity<ApiResponse<FarmDeleteCheckResponse>> checkDeleteEligibility(@PathVariable Long farmId) {
        log.info("REST request to check delete farm eligibility for Farm ID: {}", farmId);
        FarmDeleteCheckResponse responseData = deleteFarmService.checkDeleteEligibility(farmId);
        ApiResponse<FarmDeleteCheckResponse> response = ApiResponse.success(responseData, responseData.getMessage());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete")
    @Operation(summary = "Permanently Delete Farm", description = "Permanently deletes the farm, owner account, and all associated operational records in a single database transaction")
    public ResponseEntity<ApiResponse<DeleteFarmResponse>> deleteFarmPost(
            @PathVariable Long farmId,
            @Valid @RequestBody DeleteFarmRequest request) {
        log.info("REST request (POST) to delete farm ID: {}", farmId);
        DeleteFarmResponse responseData = deleteFarmService.deleteFarm(farmId, request);
        ApiResponse<DeleteFarmResponse> response = ApiResponse.success(responseData, responseData.getMessage());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "Permanently Delete Farm (DELETE method)", description = "Permanently deletes the farm, owner account, and all associated operational records in a single database transaction")
    public ResponseEntity<ApiResponse<DeleteFarmResponse>> deleteFarmDelete(
            @PathVariable Long farmId,
            @Valid @RequestBody DeleteFarmRequest request) {
        log.info("REST request (DELETE) to delete farm ID: {}", farmId);
        DeleteFarmResponse responseData = deleteFarmService.deleteFarm(farmId, request);
        ApiResponse<DeleteFarmResponse> response = ApiResponse.success(responseData, responseData.getMessage());
        return ResponseEntity.ok(response);
    }
}
