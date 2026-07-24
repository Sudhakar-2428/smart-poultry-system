package com.poultry.backend.service;

import com.poultry.backend.dto.*;
import java.time.LocalDate;

public interface ReportService {
    DashboardSummaryResponse getDashboardSummary();
    ChickenReportResponse getChickenReport(String dateFilter, LocalDate customStart, LocalDate customEnd);
    EggReportResponse getEggReport(String dateFilter, LocalDate customStart, LocalDate customEnd);
    HatchingReportResponse getHatchingReport(String dateFilter, LocalDate customStart, LocalDate customEnd);
    GrowthReportResponse getGrowthReport(String dateFilter, LocalDate customStart, LocalDate customEnd);
    BreedingReportResponse getBreedingReport(String dateFilter, LocalDate customStart, LocalDate customEnd);
    HealthAnalyticsResponse getHealthReport(String dateFilter, LocalDate customStart, LocalDate customEnd);
    FeedAnalyticsResponse getFeedReport(String dateFilter, LocalDate customStart, LocalDate customEnd);
    SalesAnalyticsResponse getSalesReport(String dateFilter, LocalDate customStart, LocalDate customEnd);
    FinanceReportResponse getFinanceReport(String dateFilter, LocalDate customStart, LocalDate customEnd);
    ProfitabilityResponse getProfitabilityReport(String dateFilter, LocalDate customStart, LocalDate customEnd);
}
