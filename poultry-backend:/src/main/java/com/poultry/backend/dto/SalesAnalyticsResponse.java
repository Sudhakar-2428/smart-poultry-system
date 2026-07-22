package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesAnalyticsResponse {
    private Double totalRevenue;
    private Double todaySales;
    private Double pendingPayments;
    private Double salesByChicken;
    private Double salesByEggBatch;

    private Map<String, Double> salesByCustomer;
    private Map<String, Double> monthlySalesTrend;
}
