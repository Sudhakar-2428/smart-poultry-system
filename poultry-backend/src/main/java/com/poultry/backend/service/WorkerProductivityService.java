package com.poultry.backend.service;

import com.poultry.backend.dto.WorkerProductivityDTOs;

import java.util.List;

public interface WorkerProductivityService {
    WorkerProductivityDTOs.WorkerProductivitySummary getTodayProductivitySummary();
    List<WorkerProductivityDTOs.LiveActivityFeedItem> getLiveActivityFeed();
    WorkerProductivityDTOs.WorkerProductivitySummary getProductivityReport(String startDate, String endDate);
}
