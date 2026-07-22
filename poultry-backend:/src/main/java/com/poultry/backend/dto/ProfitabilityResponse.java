package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitabilityResponse {
    private Double totalRevenue;
    private Double feedPurchaseCost;
    private Double otherExpenses;
    private Double grossProfit;
    private Double netProfit;
    private Double profitMarginPercentage;
}
