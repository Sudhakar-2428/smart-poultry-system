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
public class FeedAnalyticsResponse {
    private Double currentFeedStock;
    private Integer lowStockItemsCount;
    private Double totalConsumedToday;
    private Double totalPurchaseCost;

    private Map<String, Double> usageByType;
    private Map<String, Double> purchasesBySupplier;
    private Map<String, Double> monthlyConsumptionTrend;
}
