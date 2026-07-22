package com.poultry.backend.controller;

import com.poultry.backend.common.ApiResponse;
import com.poultry.backend.dto.*;
import com.poultry.backend.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports & Analytics Module", description = "Endpoints for retrieving business summaries, flock performance, egg production, health compliance, stock usages, financial ledger audits, and profitability metrics")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @Operation(summary = "Retrieve central dashboard summary overview", description = "Provides aggregated KPI metrics spanning flock sizes, egg balances, feed levels, financial revenue/expenses, and health cases. Visible only to ADMIN and MANAGER roles.")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary() {
        log.info("REST request to get dashboard summary report");
        DashboardSummaryResponse response = reportService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(response, "Dashboard overview details computed successfully"));
    }

    @GetMapping("/chickens")
    @Operation(summary = "Retrieve chickens statistics report", description = "Aggregates breed categories, ages, weight distributions, and flock level mortality rates. Supports date range filters.")
    public ResponseEntity<ApiResponse<ChickenReportResponse>> getChickenReport(
            @RequestParam(required = false, defaultValue = "last_30_days") String dateFilter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to view Chicken analytics report for range: {}", dateFilter);
        ChickenReportResponse response = reportService.getChickenReport(dateFilter, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Chicken statistics data retrieved successfully"));
    }

    @GetMapping("/eggs")
    @Operation(summary = "Retrieve egg production statistics report", description = "Displays egg production runs, damages rates, stock levels, purpose use divisions, and production timeseries trends.")
    public ResponseEntity<ApiResponse<EggReportResponse>> getEggReport(
             @RequestParam(required = false, defaultValue = "last_30_days") String dateFilter,
             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to view Egg recording analytics: {}", dateFilter);
        EggReportResponse response = reportService.getEggReport(dateFilter, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Egg production analytics report prepared successfully"));
    }

    @GetMapping("/hatching")
    @Operation(summary = "Retrieve incubator and hatching performance metrics", description = "Gives batch metrics concerning incubation parameters, performance success rates, and hatch rates across source lines.")
    public ResponseEntity<ApiResponse<HatchingReportResponse>> getHatchingReport(
            @RequestParam(required = false, defaultValue = "last_30_days") String dateFilter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to view hatching performance indicators");
        HatchingReportResponse response = reportService.getHatchingReport(dateFilter, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Hatching performance analysis retrieved successfully"));
    }

    @GetMapping("/growth")
    @Operation(summary = "Retrieve chick growth statistics", description = "Aggregates records on chick weight gain ranges, feed conversion weight progress, and growth stages progress.")
    public ResponseEntity<ApiResponse<GrowthReportResponse>> getGrowthReport(
            @RequestParam(required = false, defaultValue = "last_30_days") String dateFilter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to view growth metric reports");
        GrowthReportResponse response = reportService.getGrowthReport(dateFilter, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Flock growth performance indicators computed successfully"));
    }

    @GetMapping("/breeding")
    @Operation(summary = "Retrieve pairings and breeding analytics", description = "Presents pair configurations distribution profiles, success counts, and projected outcomes.")
    public ResponseEntity<ApiResponse<BreedingReportResponse>> getBreedingReport(
            @RequestParam(required = false, defaultValue = "last_30_days") String dateFilter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to view breeding summary reports");
        BreedingReportResponse response = reportService.getBreedingReport(dateFilter, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Breeding program performance reports compiled successfully"));
    }

    @GetMapping("/health")
    @Operation(summary = "Retrieve health records and vaccination program statistics", description = "Gives metrics on sickness rates, recoveries, mortality counts, vaccination schedule compliance index, and cases.")
    public ResponseEntity<ApiResponse<HealthAnalyticsResponse>> getHealthReport(
            @RequestParam(required = false, defaultValue = "last_30_days") String dateFilter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to get health records summary analytics");
        HealthAnalyticsResponse response = reportService.getHealthReport(dateFilter, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Health & vaccination execution reports parsed successfully"));
    }

    @GetMapping("/feed")
    @Operation(summary = "Retrieve feed inventory and stock consumption details", description = "Summarizes usages distributions across groups, supplier details summaries, and total cost conversions.")
    public ResponseEntity<ApiResponse<FeedAnalyticsResponse>> getFeedReport(
            @RequestParam(required = false, defaultValue = "last_30_days") String dateFilter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to view feed balance reports");
        FeedAnalyticsResponse response = reportService.getFeedReport(dateFilter, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Feed inventory and consumption reports prepared successfully"));
    }

    @GetMapping("/sales")
    @Operation(summary = "Retrieve sales revenue and orders metrics", description = "Focuses on transaction revenue values, customer counts, billing balances, and product distributions.")
    public ResponseEntity<ApiResponse<SalesAnalyticsResponse>> getSalesReport(
            @RequestParam(required = false, defaultValue = "last_30_days") String dateFilter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to view sales orders performance reports");
        SalesAnalyticsResponse response = reportService.getSalesReport(dateFilter, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales execution and invoice analytics reports compiled successfully"));
    }

    @GetMapping("/finance")
    @Operation(summary = "Retrieve farm financial accounting records audits", description = "Audits double-entry ledger summaries, balance flows, net cash profiles, and expense categories.")
    public ResponseEntity<ApiResponse<FinanceReportResponse>> getFinanceReport(
            @RequestParam(required = false, defaultValue = "last_30_days") String dateFilter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to retrieve financial ledger accounts summaries");
        FinanceReportResponse response = reportService.getFinanceReport(dateFilter, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Financial account and double-entry transaction reports computed successfully"));
    }

    @GetMapping("/profitability")
    @Operation(summary = "Retrieve bottom-line profitability metrics audits", description = "Tracks feed conversion cost margins versus sales return items to generate exact profitability ratios.")
    public ResponseEntity<ApiResponse<ProfitabilityResponse>> getProfitabilityReport(
            @RequestParam(required = false, defaultValue = "last_30_days") String dateFilter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to read farm cost margins report");
        ProfitabilityResponse response = reportService.getProfitabilityReport(dateFilter, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Bottom-line profitability metrics audited successfully"));
    }
}
