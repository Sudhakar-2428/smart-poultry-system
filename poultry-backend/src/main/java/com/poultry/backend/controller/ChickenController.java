package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.ChickenRequest;
import com.poultry.backend.dto.ChickenResponse;
import com.poultry.backend.dto.ChickenSummaryResponse;
import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.Gender;
import com.poultry.backend.service.ChickenService;
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
import java.util.List;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/chickens", "/chickens"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chicken Management", description = "Endpoints for managing chicken flock records, dynamic query searches, and validation rules")
public class ChickenController {

    private final ChickenService chickenService;

    @GetMapping("/next-code")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get next auto-generated Chicken ID", description = "Retrieve the next unique formatted Chicken ID (e.g. CHK-000001)")
    public ResponseEntity<ApiResponse<String>> getNextChickenCode() {
        String code = chickenService.generateNextChickenCode();
        return ResponseEntity.ok(ApiResponse.success(code, "Next chicken code generated"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'CO_OWNER', 'FARM_MANAGER', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Register a new chicken", description = "Add a new chicken to the farm system. Requires PRIMARY_OWNER, MANAGER, or ADMIN privileges.")
    public ResponseEntity<ApiResponse<ChickenResponse>> createChicken(@Valid @RequestBody ChickenRequest request) {
        log.info("REST request to register new chicken. Code: {}", request.getChickenCode());
        ChickenResponse response = chickenService.createChicken(request);
        return new ResponseEntity<>(ApiResponse.success(response, "Chicken registered successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get chicken details by ID", description = "Retrieve detailed information for a specific chicken by database ID.")
    public ResponseEntity<ApiResponse<ChickenResponse>> getChickenById(@PathVariable Long id) {
        log.info("REST request to view chicken ID: {}", id);
        ChickenResponse response = chickenService.getChickenById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Chicken details retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Update chicken details", description = "Modify values of an existing chicken record. Requires PRIMARY_OWNER, MANAGER, or ADMIN privileges.")
    public ResponseEntity<ApiResponse<ChickenResponse>> updateChicken(
            @PathVariable Long id,
            @Valid @RequestBody ChickenRequest request) {
        log.info("REST request to update chicken ID: {}", id);
        ChickenResponse response = chickenService.updateChicken(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Chicken details updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Patch chicken status and health status", description = "Quickly update the health status or overall status of a chicken.")
    public ResponseEntity<ApiResponse<ChickenResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody com.poultry.backend.dto.ChickenStatusPatchRequest request) {
        log.info("REST request to patch status for chicken ID: {}", id);
        ChickenResponse response = chickenService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Chicken status updated successfully"));
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get chicken timeline history", description = "Retrieve chronological timeline audit events for a chicken.")
    public ResponseEntity<ApiResponse<java.util.List<com.poultry.backend.dto.ChickenTimelineEventDTO>>> getChickenTimeline(@PathVariable Long id) {
        log.info("REST request to fetch timeline events for chicken ID: {}", id);
        java.util.List<com.poultry.backend.dto.ChickenTimelineEventDTO> timeline = chickenService.getChickenTimeline(id);
        return ResponseEntity.ok(ApiResponse.success(timeline, "Chicken timeline events retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'ADMIN')")
    @Operation(summary = "Soft delete a chicken", description = "Soft delete a chicken record from the registry database. Requires PRIMARY_OWNER or ADMIN.")
    public ResponseEntity<ApiResponse<Void>> deleteChicken(@PathVariable Long id) {
        log.info("REST request to soft delete chicken ID: {}", id);
        chickenService.deleteChicken(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Chicken soft deleted successfully"));
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get chicken dashboard statistics", description = "Retrieve total, healthy, sick, sold, dead, hen, rooster, category and recent registration counts.")
    public ResponseEntity<ApiResponse<com.poultry.backend.dto.ChickenDashboardStatsResponse>> getDashboardStats() {
        log.info("REST request to fetch chicken dashboard statistics");
        com.poultry.backend.dto.ChickenDashboardStatsResponse stats = chickenService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Chicken dashboard statistics retrieved successfully"));
    }

    @PostMapping("/bulk-archive")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Bulk archive chickens", description = "Bulk update selected chicken records to archived/sold state.")
    public ResponseEntity<ApiResponse<Void>> bulkArchive(@Valid @RequestBody com.poultry.backend.dto.BulkActionRequest request) {
        log.info("REST request to bulk archive {} chickens", request.getIds().size());
        chickenService.bulkArchive(request.getIds());
        return ResponseEntity.ok(ApiResponse.success(null, "Bulk archive completed successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search and filter chickens", description = "Retrieve a paginated, sorted list of chickens dynamically filtered by search term, breed, gender, category, status, healthStatus, origin, ageGroup, weight, and dynamic age limits.")
    public ResponseEntity<ApiResponse<Page<ChickenSummaryResponse>>> searchChickens(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Breed breed,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) ChickenCategory category,
            @RequestParam(required = false) ChickenStatus status,
            @RequestParam(required = false) com.poultry.backend.entity.HealthStatus healthStatus,
            @RequestParam(required = false) com.poultry.backend.entity.ChickenOrigin origin,
            @RequestParam(required = false) String ageGroup,
            @RequestParam(required = false) Integer minAgeDays,
            @RequestParam(required = false) Integer maxAgeDays,
            @RequestParam(required = false) Double minWeight,
            @RequestParam(required = false) Double maxWeight,
            @RequestParam(required = false) String chickenCode,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        log.info("REST request to search chickens with search: '{}', status: {}, category: {}", search, status, category);

        String[] sortParts = sort.split(",");
        String sortProperty = sortParts[0];
        // Normalize sort property names
        if ("newest".equalsIgnoreCase(sortProperty)) sortProperty = "id";
        else if ("oldest".equalsIgnoreCase(sortProperty)) sortProperty = "id";
        else if ("age".equalsIgnoreCase(sortProperty)) sortProperty = "dateOfBirth";
        else if ("weight".equalsIgnoreCase(sortProperty)) sortProperty = "weight";
        else if ("chickenId".equalsIgnoreCase(sortProperty)) sortProperty = "chickenCode";

        Sort sortOrder = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortProperty).ascending()
                : Sort.by(sortProperty).descending();

        if ("oldest".equalsIgnoreCase(sortParts[0])) {
            sortOrder = Sort.by("id").ascending();
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<ChickenSummaryResponse> results = chickenService.searchChickens(
                search, breed, gender, category, status, healthStatus, origin, ageGroup,
                minAgeDays, maxAgeDays, minWeight, maxWeight, chickenCode, name, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(results, "Chickens search query processed successfully"));
    }

    @PostMapping("/{id}/weight")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Update chicken weight", description = "Record current measured weight for a chicken and update growth history.")
    public ResponseEntity<ApiResponse<ChickenResponse>> updateWeight(
            @PathVariable Long id,
            @Valid @RequestBody com.poultry.backend.dto.ChickenActionDTOs.WeightUpdateRequest request) {
        log.info("REST request to update weight for chicken ID: {}", id);
        ChickenResponse response = chickenService.updateWeight(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Weight updated successfully"));
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN', 'WORKER')")
    @Operation(summary = "Transfer chicken", description = "Record transfer location/shed for a chicken.")
    public ResponseEntity<ApiResponse<ChickenResponse>> transferChicken(
            @PathVariable Long id,
            @Valid @RequestBody com.poultry.backend.dto.ChickenActionDTOs.TransferRequest request) {
        log.info("REST request to transfer chicken ID: {}", id);
        ChickenResponse response = chickenService.transferChicken(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Chicken transferred successfully"));
    }

    @PostMapping("/{id}/sell")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Sell chicken", description = "Record sale of chicken, set status to SOLD, and log audit event.")
    public ResponseEntity<ApiResponse<ChickenResponse>> sellChicken(
            @PathVariable Long id,
            @Valid @RequestBody com.poultry.backend.dto.ChickenActionDTOs.SellRequest request) {
        log.info("REST request to sell chicken ID: {}", id);
        ChickenResponse response = chickenService.sellChicken(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Chicken sold successfully"));
    }

    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'ADMIN')")
    @Operation(summary = "Permanently delete chicken", description = "Permanently removes a chicken and all related audit references.")
    public ResponseEntity<ApiResponse<Void>> hardDeleteChicken(@PathVariable Long id) {
        log.info("REST request to hard delete chicken ID: {}", id);
        chickenService.hardDeleteChicken(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Chicken permanently deleted successfully"));
    }

    @GetMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get full chicken profile report data", description = "Retrieve aggregated dataset for generating export PDF report.")
    public ResponseEntity<ApiResponse<com.poultry.backend.dto.ChickenActionDTOs.ChickenFullProfileReportDTO>> getFullProfileReport(@PathVariable Long id) {
        log.info("REST request to fetch full profile report dataset for chicken ID: {}", id);
        com.poultry.backend.dto.ChickenActionDTOs.ChickenFullProfileReportDTO report = chickenService.getFullProfileReport(id);
        return ResponseEntity.ok(ApiResponse.success(report, "Full profile report data retrieved successfully"));
    }

    @PostMapping("/{id}/pair")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ChickenResponse>> pairChicken(@PathVariable Long id, @Valid @RequestBody com.poultry.backend.dto.ChickenAdvancedDTOs.PairingActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.pairChicken(id, request), "Chicken paired successfully"));
    }

    @PostMapping("/{id}/hatch-batch")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ChickenResponse>> startHatchBatch(@PathVariable Long id, @Valid @RequestBody com.poultry.backend.dto.ChickenAdvancedDTOs.HatchBatchActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.startHatchBatch(id, request), "Hatch batch started successfully"));
    }

    @PostMapping("/{id}/hatch-result")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ChickenResponse>> recordHatchResult(@PathVariable Long id, @Valid @RequestBody com.poultry.backend.dto.ChickenAdvancedDTOs.HatchResultActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.recordHatchResult(id, request), "Hatch result recorded successfully"));
    }

    @PostMapping("/{id}/brooding")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<ChickenResponse>> moveToBrooding(@PathVariable Long id, @Valid @RequestBody com.poultry.backend.dto.ChickenAdvancedDTOs.BroodingActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.moveToBrooding(id, request), "Transferred to brooding successfully"));
    }

    @PostMapping("/{id}/death")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<ChickenResponse>> markDeath(@PathVariable Long id, @Valid @RequestBody com.poultry.backend.dto.ChickenAdvancedDTOs.DeathRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.markDeath(id, request), "Death record saved successfully"));
    }

    @PostMapping("/{id}/expense")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ChickenResponse>> addExpense(@PathVariable Long id, @Valid @RequestBody com.poultry.backend.dto.ChickenAdvancedDTOs.ExpenseActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.addExpense(id, request), "Expense recorded successfully"));
    }

    @PostMapping("/{id}/feed")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN', 'WORKER')")
    public ResponseEntity<ApiResponse<ChickenResponse>> addFeedRecord(@PathVariable Long id, @Valid @RequestBody com.poultry.backend.dto.ChickenAdvancedDTOs.FeedRecordActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.addFeedRecord(id, request), "Feed record saved successfully"));
    }

    @PostMapping("/{id}/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ChickenResponse>> capturePhoto(@PathVariable Long id, @Valid @RequestBody com.poultry.backend.dto.ChickenAdvancedDTOs.PhotoCaptureRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.capturePhoto(id, request), "Photo saved successfully"));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ChickenResponse>> archiveChicken(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.archiveChicken(id), "Chicken archived successfully"));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ChickenResponse>> restoreChicken(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.restoreChicken(id), "Chicken restored successfully"));
    }

    @PostMapping("/{id}/assign-worker")
    @PreAuthorize("hasAnyRole('PRIMARY_OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ChickenResponse>> assignWorker(@PathVariable Long id, @Valid @RequestBody com.poultry.backend.dto.ChickenAdvancedDTOs.WorkerAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.assignWorker(id, request), "Worker assigned successfully"));
    }

    @PostMapping("/{id}/reminder")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ChickenResponse>> setReminder(@PathVariable Long id, @Valid @RequestBody com.poultry.backend.dto.ChickenAdvancedDTOs.ReminderActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.setReminder(id, request), "Reminder set successfully"));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ChickenResponse>> addNote(@PathVariable Long id, @Valid @RequestBody com.poultry.backend.dto.ChickenAdvancedDTOs.ChickenNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.addNote(id, request), "Note added successfully"));
    }

    @GetMapping("/{id}/ai-analysis")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<com.poultry.backend.dto.ChickenAdvancedDTOs.AIHealthAnalysisResponse>> getAIAnalysis(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.getAIAnalysis(id), "AI health analysis retrieved"));
    }

    @GetMapping("/{id}/breeding-performance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<com.poultry.backend.dto.ChickenAdvancedDTOs.BreedingPerformanceResponse>> getBreedingPerformance(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.getBreedingPerformance(id), "Breeding performance metrics retrieved"));
    }

    @GetMapping("/{id}/market-value")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<com.poultry.backend.dto.ChickenAdvancedDTOs.MarketValueResponse>> calculateMarketValue(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.calculateMarketValue(id), "Market value calculated"));
    }

    @GetMapping("/{id}/relatives")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<com.poultry.backend.dto.ChickenAdvancedDTOs.RelatedChickensResponse>> getRelatedChickens(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.getRelatedChickens(id), "Related chickens retrieved"));
    }

    @GetMapping("/{id}/activity-log")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<com.poultry.backend.dto.ChickenAdvancedDTOs.ActivityItemDTO>>> getActivityLog(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.getActivityLog(id), "Activity log retrieved"));
    }

    @GetMapping("/{id}/audit-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<com.poultry.backend.dto.ChickenAdvancedDTOs.AuditItemDTO>>> getAuditHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(chickenService.getAuditHistory(id), "Audit history retrieved"));
    }
}

