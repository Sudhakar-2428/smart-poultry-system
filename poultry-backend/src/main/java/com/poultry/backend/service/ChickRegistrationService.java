package com.poultry.backend.service;

import com.poultry.backend.dto.ChickRegistrationDTOs;

public interface ChickRegistrationService {
    ChickRegistrationDTOs.ChickRegistrationSummaryResponse registerChicksForHatchBatch(Long incubatorBatchId);
    ChickRegistrationDTOs.ParentChickStatsResponse getParentChickStats(Long chickenId);
    ChickRegistrationDTOs.ChickReportDTO getChickReport(String reportType, Long filterId);
}
