package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.ChickenTimelineDTOs;
import com.poultry.backend.service.ChickenTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/chickens", "/chickens"})
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chicken Complete Timeline Integration", description = "Endpoints for complete chronological chicken lifecycle timeline, filters, notes, and exports")
public class ChickenTimelineController {

    private final ChickenTimelineService chickenTimelineService;


    @PostMapping("/{chickenId}/timeline/notes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add manual timeline note", description = "Allows user to add a manual note or event entry to a chicken's timeline.")
    public ResponseEntity<ApiResponse<ChickenTimelineDTOs.TimelineEventDTO>> addManualNote(
            @PathVariable Long chickenId,
            @Valid @RequestBody ChickenTimelineDTOs.CreateTimelineNoteRequest request,
            Authentication authentication) {

        String currentUser = authentication != null ? authentication.getName() : "User";
        log.info("REST request to add manual timeline note for chicken ID: {} by user: {}", chickenId, currentUser);
        ChickenTimelineDTOs.TimelineEventDTO note = chickenTimelineService.addManualNote(chickenId, request, currentUser);
        return new ResponseEntity<>(ApiResponse.success(note, "Timeline note added successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/timeline/reports")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get global timeline reports", description = "Retrieves global timeline events report across all chickens with filters.")
    public ResponseEntity<ApiResponse<ChickenTimelineDTOs.TimelineReportDTO>> getTimelineReport(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String search) {

        log.info("REST request to fetch global timeline report");
        ChickenTimelineDTOs.TimelineReportDTO report = chickenTimelineService.getTimelineReport(eventType, moduleName, startDate, endDate, search);
        return ResponseEntity.ok(ApiResponse.success(report, "Timeline report generated successfully"));
    }
}
