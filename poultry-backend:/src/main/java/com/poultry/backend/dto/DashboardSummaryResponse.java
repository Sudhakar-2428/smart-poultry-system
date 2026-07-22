package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private Long totalChickens;
    private Long activeChickens;
    private Long deadChickens;
    private Long soldChickens;
    private Long currentBrooderChicks;
    private Long totalEggsProduced;
    private Long eggsSold;
    private Long eggsAvailable;
    private Double currentFeedStock;
    private List<FeedItemResponse> lowStockItems;
    private Double todayFeedConsumption;
    private Double todaySales;
    private Double monthlyRevenue;
    private Double monthlyExpenses;
    private Double netProfit;
    private Double pendingPayments;
    private Long upcomingVaccinations;
    private Long criticalHealthCases;
}
