package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.WorkerProductivityDTOs;
import com.poultry.backend.service.WorkerProductivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/worker-productivity")
@RequiredArgsConstructor
public class WorkerProductivityController {

    private final WorkerProductivityService workerProductivityService;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<WorkerProductivityDTOs.WorkerProductivitySummary>> getTodayProductivitySummary() {
        log.info("REST request to fetch live Worker Productivity Summary");
        WorkerProductivityDTOs.WorkerProductivitySummary summary = workerProductivityService.getTodayProductivitySummary();
        return ResponseEntity.ok(ApiResponse.success(summary, "Live worker productivity summary retrieved successfully"));
    }

    @GetMapping("/activity-feed")
    public ResponseEntity<ApiResponse<List<WorkerProductivityDTOs.LiveActivityFeedItem>>> getLiveActivityFeed() {
        log.info("REST request to fetch live collection activity feed");
        List<WorkerProductivityDTOs.LiveActivityFeedItem> feed = workerProductivityService.getLiveActivityFeed();
        return ResponseEntity.ok(ApiResponse.success(feed, "Live activity feed retrieved successfully"));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<WorkerProductivityDTOs.WorkerProductivitySummary>> getProductivityReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("REST request to fetch worker productivity report for range: {} to {}", startDate, endDate);
        WorkerProductivityDTOs.WorkerProductivitySummary summary = workerProductivityService.getProductivityReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary, "Worker productivity report generated successfully"));
    }
}
