package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.EggCollectionQueueDTOs;
import com.poultry.backend.service.EggCollectionQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/egg-queue")
@RequiredArgsConstructor
public class EggCollectionQueueController {

    private final EggCollectionQueueService eggCollectionQueueService;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<EggCollectionQueueDTOs.EggQueueSummaryResponse>> getTodayQueue(Authentication authentication) {
        String currentUser = authentication != null ? authentication.getName() : "User";
        log.info("REST request to fetch today's Egg Collection Queue for user: {}", currentUser);
        EggCollectionQueueDTOs.EggQueueSummaryResponse response = eggCollectionQueueService.getTodayQueue(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Today's Egg Collection Queue retrieved successfully"));
    }

    @PostMapping("/generate-today")
    public ResponseEntity<ApiResponse<EggCollectionQueueDTOs.EggQueueSummaryResponse>> generateTodayQueue() {
        log.info("REST request to trigger 08:00 AM Daily Egg Collection Queue generation");
        EggCollectionQueueDTOs.EggQueueSummaryResponse response = eggCollectionQueueService.generateTodayQueue();
        return ResponseEntity.ok(ApiResponse.success(response, "Daily Egg Collection Queue generated successfully"));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<EggCollectionQueueDTOs.EggQueueItemResponse>> confirmQueueItem(
            @PathVariable Long id,
            @Valid @RequestBody EggCollectionQueueDTOs.ConfirmQueueItemRequest request,
            Authentication authentication) {
        String currentUser = authentication != null ? authentication.getName() : "User";
        log.info("REST request to confirm YES egg collection for Queue Item ID: {}", id);
        EggCollectionQueueDTOs.EggQueueItemResponse response = eggCollectionQueueService.confirmQueueItem(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Egg collection confirmed successfully"));
    }

    @PostMapping("/{id}/no-egg")
    public ResponseEntity<ApiResponse<EggCollectionQueueDTOs.EggQueueItemResponse>> noEggQueueItem(
            @PathVariable Long id,
            @Valid @RequestBody EggCollectionQueueDTOs.NoEggQueueItemRequest request,
            Authentication authentication) {
        String currentUser = authentication != null ? authentication.getName() : "User";
        log.info("REST request to record NO egg reason for Queue Item ID: {}", id);
        EggCollectionQueueDTOs.EggQueueItemResponse response = eggCollectionQueueService.noEggQueueItem(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "No-egg reason recorded successfully"));
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<ApiResponse<EggCollectionQueueDTOs.EggQueueItemResponse>> rescheduleQueueItem(
            @PathVariable Long id,
            @Valid @RequestBody EggCollectionQueueDTOs.RescheduleQueueItemRequest request,
            Authentication authentication) {
        String currentUser = authentication != null ? authentication.getName() : "User";
        log.info("REST request to reschedule Queue Item ID: {} for {} minutes", id, request.getDurationMinutes());
        EggCollectionQueueDTOs.EggQueueItemResponse response = eggCollectionQueueService.rescheduleQueueItem(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Reminder rescheduled successfully"));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<EggCollectionQueueDTOs.EggQueueSummaryResponse>> getQueueReport(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String breed) {
        log.info("REST request to fetch Egg Queue Reports. Status: {}, Breed: {}", status, breed);
        EggCollectionQueueDTOs.EggQueueSummaryResponse response = eggCollectionQueueService.getQueueReport(status, breed);
        return ResponseEntity.ok(ApiResponse.success(response, "Egg Collection Queue report generated successfully"));
    }
}
