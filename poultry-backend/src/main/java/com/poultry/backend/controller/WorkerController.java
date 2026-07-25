package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.*;
import com.poultry.backend.security.CustomUserDetails;
import com.poultry.backend.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v2/farms/{farmId}/workers", "/api/v1/farms/{farmId}/workers"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Worker Management", description = "Endpoints for managing farm workers")
public class WorkerController {

    private final WorkerService workerService;

    @GetMapping
    @Operation(summary = "Get all workers belonging to a farm", description = "Retrieve list of workers for the specified farm ID (Owner or Co-Owner only)")
    public ResponseEntity<ApiResponse<List<WorkerResponse>>> getWorkers(
            @PathVariable Long farmId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to list workers for farm ID: {} by user: {}", farmId, userDetails.getUsername());
        List<WorkerResponse> responseData = workerService.getWorkers(farmId, userDetails.getUsername());
        ApiResponse<List<WorkerResponse>> response = ApiResponse.success(responseData, "Workers retrieved successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Add a new worker to the farm", description = "Create a worker user account and assign them to the farm with WORKER role")
    public ResponseEntity<ApiResponse<WorkerResponse>> createWorker(
            @PathVariable Long farmId,
            @Valid @RequestBody WorkerRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to create worker in farm ID: {} by user: {}", farmId, userDetails.getUsername());
        WorkerResponse responseData = workerService.createWorker(farmId, request, userDetails.getUsername());
        ApiResponse<WorkerResponse> response = ApiResponse.success(responseData, "Worker added successfully");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/invite")
    @Operation(summary = "Invite a worker to the farm with temporary credentials", description = "Creates a worker account with temporary credentials and PENDING status")
    public ResponseEntity<ApiResponse<WorkerInviteResponse>> inviteWorker(
            @PathVariable Long farmId,
            @Valid @RequestBody WorkerInviteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to invite worker to farm ID: {} by user: {}", farmId, userDetails.getUsername());
        WorkerInviteResponse responseData = workerService.inviteWorker(farmId, request, userDetails.getUsername());
        ApiResponse<WorkerInviteResponse> response = ApiResponse.success(responseData, "Worker invited successfully");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{workerId}")
    @Operation(summary = "Update worker details", description = "Update full name, phone number, email, role, or membership status of a worker")
    public ResponseEntity<ApiResponse<WorkerResponse>> updateWorker(
            @PathVariable Long farmId,
            @PathVariable Long workerId,
            @Valid @RequestBody WorkerUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to update worker ID: {} in farm ID: {} by user: {}", workerId, farmId, userDetails.getUsername());
        WorkerResponse responseData = workerService.updateWorker(farmId, workerId, request, userDetails.getUsername());
        ApiResponse<WorkerResponse> response = ApiResponse.success(responseData, "Worker updated successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{workerId}")
    @Operation(summary = "Delete or remove worker", description = "Remove a worker's farm membership and authentication references safely")
    public ResponseEntity<ApiResponse<Void>> deleteWorker(
            @PathVariable Long farmId,
            @PathVariable Long workerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to delete worker ID: {} from farm ID: {} by user: {}", workerId, farmId, userDetails.getUsername());
        workerService.deleteWorker(farmId, workerId, userDetails.getUsername());
        ApiResponse<Void> response = ApiResponse.success(null, "Worker removed successfully");
        return ResponseEntity.ok(response);
    }
}
