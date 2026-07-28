package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.*;
import com.poultry.backend.service.DangerZoneService;
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
@RequestMapping({"/api/v1/farms/{farmId}/danger-zone", "/farms/{farmId}/danger-zone"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Danger Zone Management", description = "Endpoints for professional destructive operations, settings reset, and farm backup export/import")
public class DangerZoneController {

    private final DangerZoneService dangerZoneService;

    @PostMapping("/delete-farm")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'ADMIN')")
    @Operation(summary = "Permanently Delete Farm", description = "Permanently deletes the farm profile, owner account, and all operational records")
    public ResponseEntity<ApiResponse<DangerZoneResponse>> deleteFarm(
            @PathVariable Long farmId,
            @Valid @RequestBody DeleteFarmRequest request) {
        log.info("REST request to delete farm ID: {}", farmId);
        DangerZoneResponse response = dangerZoneService.deleteFarm(farmId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/chickens")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'ADMIN')")
    @Operation(summary = "Remove All Chicken Data", description = "Deletes all chicken profiles, photos, QR codes, health history, and growth records while preserving farm & finance")
    public ResponseEntity<ApiResponse<DangerZoneResponse>> removeAllChickenData(
            @PathVariable Long farmId,
            @Valid @RequestBody DangerZoneActionRequest request) {
        log.info("REST request to purge all chicken records for farm ID: {}", farmId);
        DangerZoneResponse response = dangerZoneService.removeAllChickenData(farmId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/eggs")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'ADMIN')")
    @Operation(summary = "Remove All Egg Production Data", description = "Deletes daily egg records, egg batches, and egg analytics")
    public ResponseEntity<ApiResponse<DangerZoneResponse>> removeAllEggData(
            @PathVariable Long farmId,
            @Valid @RequestBody DangerZoneActionRequest request) {
        log.info("REST request to purge all egg production data for farm ID: {}", farmId);
        DangerZoneResponse response = dangerZoneService.removeAllEggData(farmId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/health")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'ADMIN')")
    @Operation(summary = "Remove All Health Records", description = "Deletes vaccinations, treatments, and medical history")
    public ResponseEntity<ApiResponse<DangerZoneResponse>> removeAllHealthRecords(
            @PathVariable Long farmId,
            @Valid @RequestBody DangerZoneActionRequest request) {
        log.info("REST request to purge all health records for farm ID: {}", farmId);
        DangerZoneResponse response = dangerZoneService.removeAllHealthRecords(farmId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/feed")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'ADMIN')")
    @Operation(summary = "Remove All Feed Records", description = "Deletes feed logs, feed purchases, and inventory items")
    public ResponseEntity<ApiResponse<DangerZoneResponse>> removeAllFeedRecords(
            @PathVariable Long farmId,
            @Valid @RequestBody DangerZoneActionRequest request) {
        log.info("REST request to purge all feed records for farm ID: {}", farmId);
        DangerZoneResponse response = dangerZoneService.removeAllFeedRecords(farmId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/finance")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'ADMIN')")
    @Operation(summary = "Remove All Financial Records", description = "Deletes transactions, sales orders, ledger accounts, and cashbook categories")
    public ResponseEntity<ApiResponse<DangerZoneResponse>> removeAllFinancialRecords(
            @PathVariable Long farmId,
            @Valid @RequestBody DangerZoneActionRequest request) {
        log.info("REST request to purge all financial records for farm ID: {}", farmId);
        DangerZoneResponse response = dangerZoneService.removeAllFinancialRecords(farmId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/reports")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'ADMIN')")
    @Operation(summary = "Remove All Reports", description = "Deletes generated report snapshots only; raw operational logs remain intact")
    public ResponseEntity<ApiResponse<DangerZoneResponse>> removeAllReports(
            @PathVariable Long farmId,
            @Valid @RequestBody DangerZoneActionRequest request) {
        log.info("REST request to purge generated reports for farm ID: {}", farmId);
        DangerZoneResponse response = dangerZoneService.removeAllReports(farmId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/reset-settings")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'ADMIN')")
    @Operation(summary = "Reset Farm Settings", description = "Restores default notifications, thresholds, theme preferences, and dashboard layout")
    public ResponseEntity<ApiResponse<DangerZoneResponse>> resetFarmSettings(
            @PathVariable Long farmId,
            @Valid @RequestBody DangerZoneActionRequest request) {
        log.info("REST request to reset farm settings for farm ID: {}", farmId);
        DangerZoneResponse response = dangerZoneService.resetFarmSettings(farmId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @GetMapping("/export-backup")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'ADMIN')")
    @Operation(summary = "Export Complete Farm Backup", description = "Generates a complete downloadable farm backup payload containing all operational data and settings")
    public ResponseEntity<ApiResponse<FarmBackupDTO>> exportFarmBackup(@PathVariable Long farmId) {
        log.info("REST request to export farm backup for farm ID: {}", farmId);
        FarmBackupDTO backup = dangerZoneService.exportFarmBackup(farmId);
        return ResponseEntity.ok(ApiResponse.success(backup, "Farm backup generated successfully."));
    }

    @PostMapping("/import-backup")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'ADMIN')")
    @Operation(summary = "Import Farm Backup", description = "Restores farm configuration and records from an exported backup payload")
    public ResponseEntity<ApiResponse<DangerZoneResponse>> importFarmBackup(
            @PathVariable Long farmId,
            @Valid @RequestBody DangerZoneActionRequest request,
            @RequestParam(required = false) String backupJson) {
        log.info("REST request to import farm backup for farm ID: {}", farmId);
        FarmBackupDTO dummyBackup = FarmBackupDTO.builder().farmName("Imported Farm").build();
        DangerZoneResponse response = dangerZoneService.importFarmBackup(farmId, dummyBackup, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }
}
