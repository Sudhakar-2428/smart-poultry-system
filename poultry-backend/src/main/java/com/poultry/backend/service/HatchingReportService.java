package com.poultry.backend.service;

import com.poultry.backend.dto.HatchingReportDTOs;

public interface HatchingReportService {
    HatchingReportDTOs.HatchingReportResponse generateHatchingReport(Long incubatorBatchId);
    HatchingReportDTOs.HatchingReportResponse getReportByBatchId(Long incubatorBatchId);
}
