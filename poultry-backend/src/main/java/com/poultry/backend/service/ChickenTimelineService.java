package com.poultry.backend.service;

import com.poultry.backend.dto.ChickenTimelineDTOs;

import java.util.List;

public interface ChickenTimelineService {
    List<ChickenTimelineDTOs.TimelineEventDTO> getChickenTimeline(Long chickenId, String eventType, String moduleName, String startDate, String endDate, String search);
    ChickenTimelineDTOs.TimelineEventDTO addManualNote(Long chickenId, ChickenTimelineDTOs.CreateTimelineNoteRequest request, String currentUser);
    ChickenTimelineDTOs.TimelineReportDTO getTimelineReport(String eventType, String moduleName, String startDate, String endDate, String search);
}
