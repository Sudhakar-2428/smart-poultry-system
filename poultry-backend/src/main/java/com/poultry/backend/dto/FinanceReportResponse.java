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
public class FinanceReportResponse {
    private Double totalIncome;
    private Double totalExpense;
    private Double netProfit;
    
    private Map<String, Double> accountBalances;
    private Map<String, Double> expenseByCategory;
    private Map<String, Double> incomeByCategory;
    private Map<String, Double> monthlyIncomeTrend;
    private Map<String, Double> monthlyExpenseTrend;
}
